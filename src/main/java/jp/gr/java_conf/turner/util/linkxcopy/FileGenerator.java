package jp.gr.java_conf.turner.util.linkxcopy;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;

import jp.gr.java_conf.turner.util.Generator;

public class FileGenerator extends Generator<File> {
	final File srcRoot;
	final boolean isUseFilter;

	public FileGenerator(File srcRoot) {
		this(srcRoot, true);
	}

	public FileGenerator(File srcRoot, boolean isUseFilter) {
		super();
		this.srcRoot = srcRoot;
		this.isUseFilter = isUseFilter;
	}

	@Override
	public void run() throws InterruptedException {

		listPath(srcRoot);
	}

	private void listPath(File dir) throws InterruptedException {
		File[] fs = dir.listFiles(isUseFilter ? IMGFILE_FILTER : NULL_FILTER);
		if (fs != null) {
			for (File f : fs) {
				if (f.isDirectory()) {
					listPath(f);
				} else if (f.isFile()) {
					// System.out.println("#> " + f);
					yield(f);
				}
			}
		}
	}

	static final FileFilter NULL_FILTER = new FileFilter() {

		@Override
		public boolean accept(File f) {
			return true;
		}

	};

	static final FileFilter IMGFILE_FILTER = new FileFilter() {

		@Override
		public boolean accept(File file) {

			if (file.isDirectory()) {
				return true;
			}

			String ext = Util.getExt(file);
			if (ext != null) {
				return EXTS.contains(ext.toUpperCase());
			}

			return false;
		}
	};
	static HashSet<String> EXTS;
	static {
		EXTS = new HashSet<String>();
		String[] x = { "JPG", "JPEG", "GIF", "PNG", "BMP", "TIF", "PSD", "LNK" };
		for (String s : x) {
			EXTS.add(s);
		}
	}

}