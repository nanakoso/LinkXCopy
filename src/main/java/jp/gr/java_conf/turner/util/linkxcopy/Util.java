package jp.gr.java_conf.turner.util.linkxcopy;

import java.io.File;
import java.io.IOException;

import jp.ne.so_net.ga2.no_ji.jcom.JComException;

public class Util {

	/**
	 * @param file
	 * @param ext
	 * @return
	 */
	static File setExt(final File file, final String ext) {
		String name = getNameBody(file);
		File parentDir = file.getParentFile();
		File ret = new File(parentDir, (ext != null) ? name + "." + ext : name);

		return ret;
	}

	/**
	 * @param file
	 * @return
	 */
	static String getExt(final File file) {
		String ext = null;
		String name = file.getName();
		if (name != null) {
			int index = name.lastIndexOf('.');
			if (index >= 0) {
				ext = name.substring(index + 1);
			}
		}
		return ext;
	}

	/**
	 * @param dstFile
	 * @param srcFile
	 * @return
	 */
	static File copyExt(final File dstFile, final File srcFile) {
		String ext = getExt(srcFile);
		return setExt(dstFile, ext);
	}

	/**
	 * @param file
	 * @return
	 */
	static String getNameBody(final File file) {
		String name = file.getName();
		String prev = name;
		if (name != null) {
			int index = name.lastIndexOf('.');
			if (index >= 0) {
				prev = name.substring(0, index);
			}
		}
		return prev;
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	static void maekParentDir(final File file) throws IOException {
		File parentDir = file.getParentFile();
		if (!parentDir.exists()) {
			if (!parentDir.mkdirs()) {
				throw new IOException("cant makedirs:" + file.getParent());
			}
		}
	}

	/**
	 * @param file
	 * @return
	 */
	static boolean isLink(final File file) {
		if (file.getName() != null) {
			String postfix = Util.getExt(file);
			if (postfix != null) {
				return postfix.equalsIgnoreCase("LNK");
			}
		}
		return false;
	}

	/**
	 * @param lnkFile
	 * @param lnkParser
	 * @return targetFile
	 * @throws IOException
	 * @throws JComException
	 */
	static File traceLink(final File lnkFile, final LnkParser lnkParser)
			throws IOException, JComException {
		File targetFile = lnkParser.parse(lnkFile);
		return targetFile;
	}

	/**
	 * @param file
	 * @param srcRoot
	 * @param dstRoot
	 * @return
	 * @throws IOException
	 */
	static File changePath(final File file, final File srcRoot,
			final File dstRoot) throws IOException {
		if (file.getCanonicalFile().equals(srcRoot.getCanonicalFile())) {
			return dstRoot;
		} else {
			File srcParent = file.getParentFile();
			if (srcParent != null) {
				File dstParent = changePath(srcParent, srcRoot, dstRoot);
				if (dstParent != null) {
					return new File(dstParent, file.getName());
				}
			}
		}
		return null;
	}

	/**
	 * 引数指定したディレクトリ配下の空のサブディレクトリを削除する
	 *
	 * @param dir
	 * @return
	 */
	static void delEmptySubDir(final File dir) {
		if (!dir.isDirectory()) {
			return;
		}

		File[] files = dir.listFiles();
		if (files != null && files.length > 0) {
			for (File subDir : files) {
				delEmptyDir(subDir);
			}
		}
	}

	/**
	 * 空のディレクトリであれば削除する。
	 *
	 * 配下に空のディレクトリがある場合再帰的にすべて削除する。
	 * その結果空ディレクトリになった場合自身も削除する。
	 *
	 * @param dir
	 * @return
	 */
	private static boolean delEmptyDir(final File dir) {
		if (!dir.isDirectory()) {
			return false;
		}

		boolean allDelete = true;

		File[] files = dir.listFiles();
		if (files != null && files.length > 0) {
			for (File subDir : files) {
				if (delEmptyDir(subDir) == false) {
					allDelete = false;
				}
			}
		}

		if (allDelete) {
			System.out.println("DD " + dir);
			return dir.delete();
		} else {
			return false;
		}
	}
}
