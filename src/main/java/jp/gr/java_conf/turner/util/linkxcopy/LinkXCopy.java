package jp.gr.java_conf.turner.util.linkxcopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jp.ne.so_net.ga2.no_ji.jcom.JComException;

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

		/*
		 * 削除対象チェック用ファイル一覧取得
		 */
		List<File> delList = listDeleteFiles(cpDstRoot);

		LnkParser lnkParser = new LnkParser();
		try {
			xcopy(cpSrcRoot, cpDstRoot, lnkRootMap, lnkParser, delList);
		} finally {
			lnkParser.close();
		}

		/*
		 * 削除実行
		 */
		execDelete(delList);
		Util.delEmptySubDir(cpDstRoot);
	}

	/**
	 * @param cpSrcRoot
	 * @param cpDstRoot
	 * @param lnkRootMap
	 * @param lnkParser
	 * @throws IOException
	 * @throws JComException
	 */
	private static void xcopy(final File cpSrcRoot, final File cpDstRoot,
			final Map<File, File> lnkRootMap, final LnkParser lnkParser,
			final List<File> delList) throws IOException, JComException {

		FileGenerator g = new FileGenerator(cpSrcRoot);

		for (File cpSrcOrLnk : g) {
			File cpDst = Util.changePath(cpSrcOrLnk, cpSrcRoot, cpDstRoot);
			if (cpDst == null) {
				continue;
			}
			if (Util.isLink(cpSrcOrLnk)) {
				System.out.println("<< " + cpSrcOrLnk);
				File lnkTarget = Util.traceLink(cpSrcOrLnk, lnkParser);
				System.out.println(">> " + lnkTarget);
				lnkTarget = tryChangeRoot(lnkRootMap, lnkTarget);
				if (lnkTarget.isFile()) {
					File cpDstChgExt = Util.copyExt(cpDst, lnkTarget);
					fileCheckAndCopy(lnkTarget, cpDstChgExt, cpSrcOrLnk, delList);
				}
			} else {
				fileCheckAndCopy(cpSrcOrLnk, cpDst, null, delList);
			}
		}
	}

	/**
	 * @param srcFile
	 * @param dstFile
	 * @param delList
	 * @throws IOException
	 */
	private static void fileCopy(final File srcFile, final File dstFile,
			final List<File> delList) throws IOException {
		Util.maekParentDir(dstFile);
		FileInputStream fis = new FileInputStream(srcFile);
		FileOutputStream fos = new FileOutputStream(dstFile);
		byte[] buf = new byte[1024];
		try {
			int len;
			while ((len = fis.read(buf)) >= 0) {
				fos.write(buf, 0, len);
			}
			if (delList != null) {
				delList.remove(dstFile);
			}
		} finally {
			fis.close();
			fos.close();
		}
	}

	/**
	 * @param srcFile
	 * @param dstFile
	 * @param lnkFile
	 * @param delList
	 * @throws IOException
	 */
	private static void fileCheckAndCopy(final File srcFile, final File dstFile,
			final File lnkFile, final List<File> delList) throws IOException {
		System.out.println("<- " + srcFile);
		System.out.println("-> " + dstFile);

		if (fileCopyCheck(srcFile, dstFile, lnkFile)) {
			fileCopy(srcFile, dstFile, delList);
		} else {
			System.out.println("xx SKIP");
		}

		if (delList != null) {
			delList.remove(dstFile);
		}

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

	private static List<File> listDeleteFiles(final File cpDstRoot) {
		FileGenerator g = new FileGenerator(cpDstRoot, false);
		List<File> delList = new LinkedList<File>();
		for (File f : g) {
			delList.add(f);
		}
		return delList;
	}

	private static void execDelete(final List<File> delList) {
		for (File f : delList) {
			System.out.println("D> " + f);
			f.delete();
		}
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
