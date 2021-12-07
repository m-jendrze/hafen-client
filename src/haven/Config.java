/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.rx.Reactor;
import integrations.mapv4.MappingClient;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    public static final File HOMEDIR = new File("").getAbsoluteFile();
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final Properties jarprops = getjarprops();
    public static String authuser = getprop("haven.authuser", null);
    public static String authserv = getprop("haven.authserv", null);
    public static String defserv = getprop("haven.defserv", "localhost");
    public static String[] servargs = null;
    public static URL resurl = geturl("haven.resurl", "http://game.havenandhearth.com/res/");
    public static URL screenurl = geturl("haven.screenurl", "http://game.havenandhearth.com/mt/ss");
    public static URL cachebase = geturl("haven.cachebase", "http://game.havenandhearth.com/render/");
    public static URL mapbase = geturl("haven.mapbase", "http://game.havenandhearth.com/hres/");
    public static boolean headless = false;
    public static boolean dbtext = getbool("haven.dbtext", false);
    public static boolean bounddb = getbool("haven.bounddb", false);
    public static boolean profile = getbool("haven.profile", false);
    public static boolean profilegpu = getbool("haven.profilegpu", false);
    public static boolean par = true;
    public static boolean fscache = getbool("haven.fscache", true);
    public static Path resdir = getpath("haven.resdir", System.getenv("HAFEN_RESDIR"));
    public static boolean nopreload = getbool("haven.nopreload", true);
    public static Path loadwaited = getpath("haven.loadwaited", null);
    public static Path allused = getpath("haven.allused", null);
    public static int mainport = getint("haven.mainport", 1870);
    public static int authport = getint("haven.authport", 1871);
    public static boolean softres = getbool("haven.softres", true);
    public static Double uiscale = getfloat("haven.uiscale", null);
    public static byte[] authck = getbytes("haven.authck", null), inittoken = getbytes("haven.inittoken", null);
    public static String prefspec = getprop("haven.prefspec", "hafen");
    public static final String confid = "ender";
    
    public static String version;
    public static final boolean isUpdate;
    public static boolean center_tile = false;
    private static String username, playername;
    public static boolean always_true = true; //always true to facilitate changes with minimal intrusions into loftar's code
    
    static {
	loadBuildVersion();
	isUpdate = !CFG.VERSION.get().equals(version) || !getFile("changelog.txt").exists();
	if(isUpdate){
	    CFG.VERSION.set(version);
	}
    }

    private static void loadBuildVersion() {
	InputStream in = Config.class.getResourceAsStream("/buildinfo");
	try {
	    try {
		if(in != null) {
		    Properties info = new Properties();
		    info.load(in);
		    version = info.getProperty("version");
		}
	    } finally {
		if (in != null) { in.close(); }
	    }
	} catch(IOException e) {
	    throw(new Error(e));
	}
    }

    public static File getFile(String name) {
	return new File(HOMEDIR, name);
    }

    public static String loadFile(String name) {
	InputStream inputStream = getFSStream(name);
	if(inputStream == null) {
	    inputStream = getJarStream(name);
	}
	return getString(inputStream);
    }

    public static String loadJarFile(String name) {
	return getString(getJarStream(name));
    }

    public static String loadFSFile(String name) {
	return getString(getFSStream(name));
    }

    private static InputStream getFSStream(String name) {
	InputStream inputStream = null;
	File file = Config.getFile(name);
	if(file.exists() && file.canRead()) {
	    try {
		inputStream = new FileInputStream(file);
	    } catch (FileNotFoundException ignored) {
	    }
	}
	return inputStream;
    }

    private static InputStream getJarStream(String name) {
	if(name.charAt(0) != '/') {
	    name = '/' + name;
	}
	return Config.class.getResourceAsStream(name);
    }

    private static String getString(InputStream inputStream) {
	if(inputStream != null) {
	    try {
		return Utils.stream2str(inputStream);
	    } catch (Exception ignore) {
	    } finally {
		try {inputStream.close();} catch (IOException ignored) {}
	    }
	}
	return null;
    }

    public static void saveFile(String name, String data) {
	File file = Config.getFile(name);
	boolean exists = file.exists();
	if(!exists) {
	    try {
		String parent = file.getParent();
		//noinspection ResultOfMethodCallIgnored
		new File(parent).mkdirs();
		exists = file.createNewFile();
	    } catch (IOException ignored) {}
	}
	if(exists && file.canWrite()) {
	    try (FileOutputStream fos = new FileOutputStream(file);
		 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
		 BufferedWriter writer = new BufferedWriter(osw)) {
		writer.write(data);
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    private static Properties getjarprops() {
	Properties ret = new Properties();
	try(InputStream fp = Config.class.getResourceAsStream("boot-props")) {
	    if(fp != null)
		ret.load(fp);
	} catch(Exception exc) {
	    /* XXX? Catch all exceptions? It just seems dumb to
	     * potentially crash here for unforeseen reasons. */
	    new Warning(exc, "unexpected error occurred when loading local properties").issue();
	}
	try {
	    Path jar = Utils.srcpath(Config.class);
	    if(jar != null) {
		try(InputStream fp = Files.newInputStream(jar.resolveSibling("haven-config.properties"))) {
		    ret.load(fp);
		} catch(NoSuchFileException exc) {
		    /* That's quite alright. */
		}
	    }
	} catch(Exception exc) {
	    new Warning(exc, "unexpected error occurred when loading neighboring properties").issue();
	}
	return(ret);
    }

    private static String getprop(String name, String def) {
	String ret = jarprops.getProperty(name);
	if(ret != null)
	    return(ret);
	return(Utils.getprop(name, def));
    }
    
    private static int getint(String name, int def) {
	String val = getprop(name, null);
	if(val == null)
	    return(def);
	return(Integer.parseInt(val));
    }

    private static boolean getbool(String name, boolean def) {
	String val = getprop(name, null);
	if(val == null)
	    return(def);
	return(Utils.parsebool(val));
    }

    private static byte[] getbytes(String name, byte[] def) {
	String val = getprop(name, null);
	if(val == null)
	    return(def);
	return(Utils.hex2byte(val));
    }

    private static URL geturl(String name, String def) {
	String val = getprop(name, def);
	if(val.equals(""))
	    return(null);
	try {
	    return(new URL(val));
	} catch(java.net.MalformedURLException e) {
	    throw(new RuntimeException(e));
	}
    }

    private static Path getpath(String name, String def) {
	String val = getprop(name, def);
	if((val == null) || val.equals(""))
	    return(null);
	return(Utils.path(val));
    }

    private static Double getfloat(String name, Double def) {
	String val = getprop(name, null);
	if(val == null)
	    return(def);
	return(Double.parseDouble(val));
    }

    private static void usage(PrintStream out) {
	out.println("usage: haven.jar [OPTIONS] [SERVER[:PORT]]");
	out.println("Options include:");
	out.println("  -h                 Display this help");
	out.println("  -n                 Headless");
	out.println("  -d                 Display debug text");
	out.println("  -P                 Enable profiling");
	out.println("  -G                 Enable GPU profiling");
	out.println("  -U URL             Use specified external resource URL");
	out.println("  -r DIR             Use specified resource directory (or HAVEN_RESDIR)");
	out.println("  -A AUTHSERV[:PORT] Use specified authentication server");
	out.println("  -u USER            Authenticate as USER (together with -C)");
	out.println("  -C HEXCOOKIE       Authenticate with specified hex-encoded cookie");
	out.println("  -p PREFSPEC        Use alternate preference prefix");
    }

    public static void cmdline(String[] args) {
	PosixArgs opt = PosixArgs.getopt(args, "hndPGU:r:A:u:C:p:");
	if(opt == null) {
	    usage(System.err);
	    System.exit(1);
	}
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'h':
		usage(System.out);
		System.exit(0);
		break;
	    case 'n':
		headless = true;
		break;
	    case 'd':
		dbtext = true;
		break;
	    case 'P':
		profile = true;
		break;
	    case 'G':
		profilegpu = true;
		break;
	    case 'r':
		resdir = Utils.path(opt.arg);
		break;
	    case 'A':
		int p = opt.arg.indexOf(':');
		if(p >= 0) {
		    authserv = opt.arg.substring(0, p);
		    authport = Integer.parseInt(opt.arg.substring(p + 1));
		} else {
		    authserv = opt.arg;
		}
		break;
	    case 'U':
		try {
		    resurl = new URL(opt.arg);
		} catch(java.net.MalformedURLException e) {
		    System.err.println(e);
		    System.exit(1);
		}
		break;
	    case 'u':
		authuser = opt.arg;
		break;
	    case 'C':
		authck = Utils.hex2byte(opt.arg);
		break;
	    case 'p':
		prefspec = opt.arg;
		break;
	    }
	}
	if(opt.rest.length > 0) {
	    int p = opt.rest[0].indexOf(':');
	    if(p >= 0) {
		defserv = opt.rest[0].substring(0, p);
		mainport = Integer.parseInt(opt.rest[0].substring(p + 1));
	    } else {
		defserv = opt.rest[0];
	    }
	}
	if(opt.rest.length > 1)
	    servargs = Utils.splice(opt.rest, 1);
    }
    
    public static void setUserName(String username) {
	Config.username = username;
	Config.playername = null;
    }
    
    public static void setPlayerName(String playername) {
	Config.playername = playername;
	Reactor.PLAYER.onNext(userpath());
    }
    
    public static String userpath() {
	return String.format("%s/%s", username, playername);
    }

    static {
	Console.setscmd("stats", new Console.Command() {
		public void run(Console cons, String[] args) {
		    dbtext = Utils.parsebool(args[1]);
		}
	    });
	Console.setscmd("par", new Console.Command() {
		public void run(Console cons, String[] args) {
		    par = Utils.parsebool(args[1]);
		}
	    });
	Console.setscmd("profile", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if(args[1].equals("none") || args[1].equals("off")) {
			profile = profilegpu = false;
		    } else if(args[1].equals("cpu")) {
			profile = true;
		    } else if(args[1].equals("gpu")) {
			profilegpu = true;
		    } else if(args[1].equals("all")) {
			profile = profilegpu = true;
		    }
		}
	    });
    }
    
    public static void initAutomapper(UI ui) {
        if (MappingClient.initialized()) {
            MappingClient.destroy();
	}
	MappingClient.init(ui.sess.glob);
	MappingClient automapper = MappingClient.getInstance();
	automapper.SetPlayerName(playername);
	automapper.SetEndpoint(CFG.AUTOMAP_ENDPOINT.get());
	automapper.EnableGridUploads(CFG.AUTOMAP_UPLOAD.get());
	automapper.EnableTracking(CFG.AUTOMAP_TRACK.get());
    }
}
