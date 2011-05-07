package jp.gr.java_conf.turner.util.linkxcopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jp.ne.so_net.ga2.no_ji.jcom.JComException;

class CopyTaple {
	public CopyTaple(File src, File dst) {
		this.src = src;
		this.dst = dst;
	}

	private File src;

	public File getSrc() {
		return src;
	}

	private File dst;

	public File getDst() {
		return dst;
	}

	/**
	 * @param srcFile
	 * @param dstFile
	 * @param lnkFile
	 * @return
	 */
	private static boolean fileCopyCheck(final File srcFile,
			final File dstFile, final File lnkFile) {
		// コピー先が存在しなかったらコピー
		if (!dstFile.exists()) {
			return true;
		}
		// ソースのほうが新しかったらコピー
		if (srcFile.lastModified() >= dstFile.lastModified()) {
			return true;
		}
		// ファイルサイズが違ったらコピー
		if (srcFile.length() != dstFile.length()) {
			return true;
		}
		// ショートカットのほうが新しかったらコピー
		if (lnkFile != null && lnkFile.lastModified() >= dstFile.lastModified()) {
			return true;
		}
		return false;
	}

	/**
	 * @param srcFile
	 * @param dstFile
	 * @param delList
	 * @throws IOException
	 */
	private static void fileCopy(final File srcFile, final File dstFile)
			throws IOException {
		Util.maekParentDir(dstFile);
		FileChannel fic = new FileInputStream(srcFile).getChannel();
		FileChannel foc = new FileOutputStream(dstFile).getChannel();
		try {
			fic.transferTo(0, fic.size(), foc);
		} finally {
			fic.close();
			foc.close();
		}
	}

	/**
	 * @param srcFile
	 * @param dstFile
	 * @param lnkFile
	 * @param delList
	 * @throws IOException
	 */
	boolean fileCheckAndCopy(final File lnkFile) throws IOException {
		final File srcFile = src;
		final File dstFile = dst;
		if (fileCopyCheck(srcFile, dstFile, lnkFile)) {
			fileCopy(srcFile, dstFile);
			System.out.println("<C " + srcFile);
			System.out.println("C> " + dstFile);
			return true;
		} else {
			System.out.println("<S " + srcFile);
			System.out.println("S> " + dstFile);
			return false;
		}

	}
}

/**
 * @author nanakoso
 *
 */
public class LinkXCopy {

	/**
	 * @param args
	 * @throws IOException
	 * @throws JComException
	 */
	public static void main(String[] args) throws IOException, JComException {

		final File cpSrcRoot = new File(args[0]);
		final File cpDstRoot = new File(args[1]);
		final Map<File, File> lnkRootMap;
		if (args.length >= 4) {
			final String lnkSrcRoots = args[2];
			final String lnkDstRoots = args[3];
			lnkRootMap = makeMap(lnkSrcRoots, lnkDstRoots);
		} else {
			lnkRootMap = new LinkedHashMap<File, File>();
		}

		System.out.println("start get src list");
		List<File> srcList = listFiles(cpSrcRoot, true);
		/*
		 * 削除対象チェック用ファイル一覧取得
		 */
		System.out.println("start get dst list");
		List<File> delList = listFiles(cpDstRoot, false);

		TreeMap<File, File> cpList = makeCpList(srcList, cpSrcRoot, cpDstRoot);


		/*
		 * コピー名候補作成
		 */
		LnkParser lnkParser = new LnkParser();
		TreeMap<File, CopyTaple> copyMap;
		try {
			copyMap = makeXCopyMap(cpList, lnkRootMap, lnkParser, delList);
		} finally {
			lnkParser.close();
		}

		/*
		 * 駄目文字リネーム
		 */
		System.out.println("start rename bad name");
		TreeMap<File, CopyTaple> renamedCopyMap = renameBadName(copyMap);

		/*
		 * 重複リネーム
		 */
		System.out.println("start rename dup name");
		TreeMap<File, CopyTaple> dupRenCopyMap = renameDupName(renamedCopyMap);

		/*
		 * コピー
		 */
		for (File cpSrcOrLnk : dupRenCopyMap.keySet()) {
			CopyTaple cp = dupRenCopyMap.get(cpSrcOrLnk);
			cp.fileCheckAndCopy(cpSrcOrLnk);
			delList.remove(cp.getDst());
		}

		/*
		 * 削除実行
		 */
		execDelete(delList);
		Util.delEmptySubDir(cpDstRoot);
	}

	private static TreeMap<File, CopyTaple> renameDupName(
			TreeMap<File, CopyTaple> renamedCopyMap) {
		TreeMap<File, CopyTaple> dupRenCopyMap = new TreeMap<File, CopyTaple>();
		Set<File> fileSet = new HashSet<File>();
		for (File key : renamedCopyMap.keySet()) {
			CopyTaple cp = renamedCopyMap.get(key);
			File renamed = Util.renameToAvoidDupName(cp.getDst(), fileSet);
			dupRenCopyMap.put(key, new CopyTaple(cp.getSrc(), renamed));
			fileSet.add(renamed);
		}
		return dupRenCopyMap;
	}

	private static TreeMap<File, CopyTaple> renameBadName(
			TreeMap<File, CopyTaple> copyMap) {
		TreeMap<File, CopyTaple> renamedCopyMap = new TreeMap<File, CopyTaple>();
		for (File cpSrcOrLnk : copyMap.keySet()) {
			CopyTaple cp = copyMap.get(cpSrcOrLnk);
			File dst = renameBadCharPath(cp.getDst());
			renamedCopyMap.put(cpSrcOrLnk, new CopyTaple(cp.getSrc(), dst));
		}
		return renamedCopyMap;
	}

	private static TreeMap<File, File> makeCpList(List<File> srcList,
			File cpSrcRoot, File cpDstRoot) throws IOException {
		TreeMap<File, File> map = new TreeMap<File, File>();
		for (File s : srcList) {
			File d = Util.changePath(s, cpSrcRoot, cpDstRoot);
			if (d != null) {
				map.put(s, d);
			}
		}
		return map;
	}

	/**
	 * @param cpSrcRoot
	 * @param cpDstRoot
	 * @param lnkRootMap
	 * @param lnkParser
	 * @throws IOException
	 * @throws JComException
	 */
	private static TreeMap<File, CopyTaple> makeXCopyMap(
			TreeMap<File, File> cpMap, final Map<File, File> lnkRootMap,
			final LnkParser lnkParser, final List<File> delList)
			throws IOException, JComException {

		TreeMap<File, CopyTaple> cpLnkTracedMap = new TreeMap<File, CopyTaple>();

		for (File cpSrcOrLnk : cpMap.keySet()) {
			File cpDst = cpMap.get(cpSrcOrLnk);
			if (Util.isLink(cpSrcOrLnk)) {
				System.out.println("<< " + cpSrcOrLnk);
				File lnkTarget = Util.traceLink(cpSrcOrLnk, lnkParser);
				System.out.println(">> " + lnkTarget);
				lnkTarget = tryChangeRoot(lnkRootMap, lnkTarget);
				if (lnkTarget.isFile()) {
					File cpDstChgExt = Util.copyExt(cpDst, lnkTarget);
					System.out.println("<- " + cpSrcOrLnk);
					System.out.println("-> " + cpDstChgExt);
					cpLnkTracedMap.put(cpSrcOrLnk, new CopyTaple(lnkTarget,
							cpDstChgExt));
				}
			} else {
				System.out.println("<- " + cpSrcOrLnk);
				System.out.println("-> " + cpDst);
				cpLnkTracedMap
						.put(cpSrcOrLnk, new CopyTaple(cpSrcOrLnk, cpDst));
			}
		}

		return cpLnkTracedMap;
	}

	private static List<File> listFiles(final File cpDstRoot, boolean useFilter) {
		FileGenerator g = new FileGenerator(cpDstRoot, useFilter);
		List<File> list = new LinkedList<File>();
		for (File f : g) {
			list.add(f);
		}
		return list;
	}

	private static void execDelete(final List<File> delList) {
		for (File f : delList) {
			if (!".nomedia".equals(f.getName())) {
				System.out.println("D> " + f);
				f.delete();
			} else {
				System.out.println("E> " + f);
			}
		}
	}

	static Set<File> roots = new HashSet<File>();
	static {
		for (File f : File.listRoots()) {
			roots.add(f);
		}
	}

	/**
	 * @param file
	 */
	private static File renameBadCharPath(final File file) {
		File wkFile = file;
		File p = file.getParentFile();
		if (p != null && !roots.contains(p)) {
			p = renameBadCharPath(p);
			wkFile = new File(p, file.getName());
		}

		return Util.renameToAvoidBadChar(wkFile);
	}

	/**
	 * @param map
	 * @param f
	 * @return
	 * @throws IOException
	 */
	private static File tryChangeRoot(final Map<File, File> map,
			final File orgFile) throws IOException {
		File f = orgFile;
		if (map != null) {
			for (File srcRoot : map.keySet()) {
				File dstRoot = map.get(srcRoot);
				File changes = Util.changePath(f, srcRoot, dstRoot);
				if (changes != null) {
					f = changes;
					break;
				}
			}
		}
		return f;
	}

	/**
	 * @param srcRoots
	 * @param dstRoots
	 * @return
	 */
	private static Map<File, File> makeMap(final String srcRoots,
			final String dstRoots) {

		String[] src = srcRoots.split(File.pathSeparator, -1);
		String[] dst = dstRoots.split(File.pathSeparator, -1);
		Map<File, File> map = new LinkedHashMap<File, File>();
		for (int i = 0; i < src.length && i < dst.length; i++) {
			String s = src[i];
			String d = dst[i];
			if (!s.endsWith(File.separator)) {
				s += File.separator;
			}
			if (!d.endsWith(File.separator)) {
				d += File.separator;
			}
			map.put(new File(s), new File(d));
		}
		return map;
	}

}
