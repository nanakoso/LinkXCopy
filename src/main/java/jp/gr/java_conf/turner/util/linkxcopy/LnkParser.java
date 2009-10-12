package jp.gr.java_conf.turner.util.linkxcopy;

import java.io.File;

import jp.ne.so_net.ga2.no_ji.jcom.IDispatch;
import jp.ne.so_net.ga2.no_ji.jcom.JComException;
import jp.ne.so_net.ga2.no_ji.jcom.ReleaseManager;

public class LnkParser {

	private final ReleaseManager rm;
	private final IDispatch comWS;

	public LnkParser() throws JComException {
		rm = new ReleaseManager();
		comWS = new IDispatch(rm, "Wscript.Shell");
	}

	public File parse(File f) throws JComException {
		Object[] param = new Object[] { f.getPath() };
		IDispatch comSC = (IDispatch) comWS.method("CreateShortcut", param);
		String targetPath = (String) comSC.get("TargetPath");
		File ret = new File(targetPath);
		return ret;
	}

	public void close() {
		rm.release();
	}
}
