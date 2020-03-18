/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;

import java.awt.Component;
import java.io.FileWriter;
import java.io.Reader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Stack;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.cburch.logisim.std.Builtin;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.MacCompatibility;
import com.cburch.logisim.util.StringUtil;

public class Loader implements LibraryLoader {
    public static final String LOGISIM_EXTENSION = ".circ";
    public static final FileFilter LOGISIM_FILTER = new LogisimFileFilter();
    public static final FileFilter JAR_FILTER = new JarFileFilter();

    private static class LogisimFileFilter extends FileFilter {
        public boolean accept(File f) {
            return f.isDirectory()
                || f.getName().endsWith(LOGISIM_EXTENSION);
        }

        public String getDescription() {
            return Strings.get("logisimFileFilter");
        }
    }

    private static class JarFileFilter extends FileFilter {
        public boolean accept(File f) {
            return f.isDirectory()
                || f.getName().endsWith(".jar");
        }

        public String getDescription() {
            return Strings.get("jarFileFilter");
        }
    }
    
    // fixed
    private Component parent;
    private Builtin builtin = new Builtin();

    // to be cleared with each new file
    private File mainFile = null;
    private Stack filesOpening = new Stack(); // of Files

    public Loader(Component parent) {
        this.parent = parent;
        clear();
    }
    
    public Builtin getBuiltin() {
        return builtin;
    }
    
    public void setParent(Component value) {
        parent = value;
    }

    //
    // file chooser related methods
    //
    public File getMainFile() {
        return mainFile;
    }

    public JFileChooser createChooser() {
        JFileChooser ret = new JFileChooser();
        ret.setCurrentDirectory(getCurrentDirectory());
        return ret;
    }

    // used here and in LibraryManager only
    File getCurrentDirectory() {
        File ref;
        if(!filesOpening.empty()) {
            ref = (File) filesOpening.peek();
        } else {
            ref = mainFile;
        }
        return ref == null ? null : ref.getParentFile();
    }

    private void setMainFile(File value) {
        mainFile = value;
    }

    //
    // more substantive methods accessed from outside this package
    //
    public void clear() {
        filesOpening.clear();
        mainFile = null;
    }

    public LogisimFile openLogisimFile(File file) throws LoadFailedException {
        try {
            LogisimFile ret = loadLogisimFile(file);
            if(ret != null) setMainFile(file);
            showMessages(ret);
            return ret;
        } catch(LoaderException e) {
            throw new LoadFailedException(e.getMessage());
        }
    }
    
    public LogisimFile openLogisimFile(Reader reader) throws LoadFailedException, IOException {
        LogisimFile ret = null;
        try {
            ret = LogisimFile.load(reader, this);
        } catch(LoaderException e) {
            return null;
        }
        showMessages(ret);
        return ret;
    }

    public Library loadLogisimLibrary(File file) {
        LoadedLibrary ret = LibraryManager.instance.loadLogisimLibrary(this, file);
        if(ret != null) {
            LogisimFile retBase = (LogisimFile) ret.getBase();
            showMessages(retBase);
        }
        return ret;
    }
    
    public Library loadJarLibrary(File file, String className) {
        return LibraryManager.instance.loadJarLibrary(this, file, className);
    }
    
    public void reload(LoadedLibrary lib) {
        LibraryManager.instance.reload(this, lib);
    }
    
    public boolean save(LogisimFile file, File dest) {
        Library reference = LibraryManager.instance.findReference(file, dest);
        if(reference != null) {
            JOptionPane.showMessageDialog(parent,
                    StringUtil.format(Strings.get("fileCircularError"), reference.getDisplayName()),
                    Strings.get("fileSaveErrorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        FileWriter fwrite = null;
        try {
            try {
                MacCompatibility.setFileCreatorAndType(dest, "LGSM", "circ");
            } catch(IOException e) { }
            fwrite = new FileWriter(dest);
            file.write(fwrite, this);
            file.setName(toProjectName(dest));

            File oldFile = getMainFile();
            setMainFile(dest);
            LibraryManager.instance.fileSaved(this, dest, oldFile, file);
        } catch(IOException e) {
            JOptionPane.showMessageDialog(parent,
                StringUtil.format(Strings.get("fileSaveError"),
                    e.toString()),
                Strings.get("fileSaveErrorTitle"),
                JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            if(fwrite != null) {
                try {
                    fwrite.close();
                } catch(IOException e) {
                    JOptionPane.showMessageDialog(parent,
                        StringUtil.format(Strings.get("fileSaveCloseError"),
                            e.toString()),
                        Strings.get("fileSaveErrorTitle"),
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        return true;
    }

    //
    // methods for LibraryManager
    //  
    LogisimFile loadLogisimFile(File file) throws LoadFailedException {
        for(int i = 0; i < filesOpening.size(); i++) {
            if(filesOpening.get(i).equals(file)) {
                throw new LoadFailedException(StringUtil.format(Strings.get("logisimCircularError"),
                        toProjectName(file)));
            }
        }

        Reader reader = null;
        LogisimFile ret = null;
        filesOpening.push(file);
        try {
            reader = new FileReader(file);
            ret = LogisimFile.load(reader, this);
        } catch(IOException e) {
            throw new LoadFailedException(StringUtil.format(Strings.get("logisimLoadError"),
                    toProjectName(file), e.toString()));
        } finally {
            filesOpening.pop();
            if(reader != null) {
                try {
                    reader.close();
                } catch(IOException e) {
                    throw new LoadFailedException(StringUtil.format(Strings.get("logisimLoadError"),
                            toProjectName(file), e.toString()));
                }
            }
        }
        ret.setName(toProjectName(file));
        return ret;
    }

    Library loadJarFile(File file, String className) throws LoadFailedException {
        // get class loader
        URL url;
        try {
            url = new URL("file", "localhost", file.getCanonicalPath());
        } catch(MalformedURLException e1) {
            throw new LoadFailedException("Internal error: Malformed URL");
        } catch(IOException e1) {
            throw new LoadFailedException(Strings.get("jarNotOpenedError"));
        }
        URLClassLoader loader = new URLClassLoader(new URL[] { url });
        
        // load library class from loader
        Class retClass;
        try {
            retClass = loader.loadClass(className);
        } catch(ClassNotFoundException e) {
            throw new LoadFailedException(StringUtil.format(Strings.get("jarClassNotFoundError"), className));
        }
        if(!(Library.class.isAssignableFrom(retClass))) {
            throw new LoadFailedException(StringUtil.format(Strings.get("jarClassNotLibraryError"), className));
        }
        
        // instantiate library
        Library ret;
        try {
            ret = (Library) retClass.newInstance();
        } catch(Exception e) {
            throw new LoadFailedException(StringUtil.format(Strings.get("jarLibraryNotCreatedError"), className));
        }
        return ret;
    }

    //
    // Library methods
    //
    public Library loadLibrary(String desc) {
        return LibraryManager.instance.loadLibrary(this, desc);
    }

    public String getDescriptor(Library lib) {
        return LibraryManager.instance.getDescriptor(this, lib);
    }

    public void showError(String description) {
        if(!filesOpening.empty()) {
            File top = (File) filesOpening.peek();
            description = toProjectName(top) + ": " + description;
        }
        JOptionPane.showMessageDialog(parent, description,
                Strings.get("fileErrorTitle"), JOptionPane.ERROR_MESSAGE);
        throw new LoaderException(description);
    }

    private void showMessages(LogisimFile source) {
        if(source == null) return;
        while(true) {
            String message = source.getMessage();
            if(message == null) break;

            JOptionPane.showMessageDialog(parent,
                message, Strings.get("fileMessageTitle"),
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    //
    // helper methods
    //
    File getFileFor(String name, FileFilter filter) {
        // Determine the actual file name.
        File file = new File(name);
        if(!file.isAbsolute()) {
            File currentDirectory = getCurrentDirectory();
            if(currentDirectory != null) file = new File(currentDirectory, name);
        }
        while(!file.canRead()) {
            // It doesn't exist. Figure it out from the user.
            JOptionPane.showMessageDialog(parent,
                StringUtil.format(Strings.get("fileLibraryMissingError"),
                    file.getName()));
            JFileChooser chooser = createChooser();
            chooser.setFileFilter(filter);
            chooser.setDialogTitle(StringUtil.format(Strings.get("fileLibraryMissingTitle"), file.getName()));
            int action = chooser.showDialog(parent, Strings.get("fileLibraryMissingButton"));
            if(action != JFileChooser.APPROVE_OPTION) {
                throw new LoaderException(Strings.get("fileLoadCanceledError"));
            }
            file = chooser.getSelectedFile();
        }
        return file;
    }

    private String toProjectName(File file) {
        String ret = file.getName();
        if(ret.endsWith(LOGISIM_EXTENSION)) {
            return ret.substring(0, ret.length() - LOGISIM_EXTENSION.length());
        } else {
            return ret;
        }
    }

}
