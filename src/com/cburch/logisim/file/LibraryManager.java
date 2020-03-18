/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.WeakHashMap;

import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.StringUtil;

class LibraryManager {
    public static final LibraryManager instance = new LibraryManager();

    private static char desc_sep = '#';
    
    private static class JarDescriptor {
        private File file;
        private String className;
        JarDescriptor(File file, String className) { this.file = file; this.className = className; }
        public boolean equals(Object other) {
            if(!(other instanceof JarDescriptor)) return false;
            JarDescriptor o = (JarDescriptor) other;
            return this.file.equals(o.file) && this.className.equals(o.className);
        }
        public int hashCode() {
            return file.hashCode() * 31 + className.hashCode();
        }
    }
    
    private HashMap fileMap = new HashMap(); // File/JarDescriptor -> WeakReference<LoadedLibrary>
    private WeakHashMap invMap = new WeakHashMap(); // LoadedLibrary -> File/JarDescriptor

    private LibraryManager() {
        ProjectsDirty.initialize();
    }
    
    void setDirty(File file, boolean dirty) {
        LoadedLibrary lib = findKnown(file);
        if(lib != null) {
            lib.setDirty(dirty);
        }
    }
    
    Collection getLogisimLibraries() {
        ArrayList ret = new ArrayList();
        for(Iterator it = invMap.keySet().iterator(); it.hasNext(); ) {
            LoadedLibrary lib = (LoadedLibrary) it.next();
            if(lib.getBase() instanceof LogisimFile) ret.add(lib.getBase());
        }
        return ret;
    }

    public Library loadLibrary(Loader loader, String desc) {
        // It may already be loaded.
        // Otherwise we'll have to decode it.
        int sep = desc.indexOf(desc_sep);
        if(sep < 0) {
            loader.showError(StringUtil.format(Strings.get("fileDescriptorError"), desc));
            return null;
        }
        String type = desc.substring(0, sep);
        String name = desc.substring(sep + 1);

        if(type.equals("")) {
            Library ret = loader.getBuiltin().getLibrary(name);
            if(ret == null) {
                loader.showError(StringUtil.format(Strings.get("fileBuiltinMissingError"), name));
                return null;
            }
            return ret;
        } else if(type.equals("file")) {
            File toRead = loader.getFileFor(name, Loader.LOGISIM_FILTER);
            return loadLogisimLibrary(loader, toRead);
        } else if(type.equals("jar")) {
            int sepLoc = name.lastIndexOf(desc_sep);
            String fileName = name.substring(0, sepLoc);
            String className = name.substring(sepLoc + 1);
            File toRead = loader.getFileFor(fileName, Loader.JAR_FILTER);
            return loadJarLibrary(loader, toRead, className);
        } else {
            loader.showError(StringUtil.format(Strings.get("fileTypeError"),
                type, desc));
            return null;
        }
    }
    
    public LoadedLibrary loadLogisimLibrary(Loader loader, File toRead) {
        LoadedLibrary ret = findKnown(toRead);
        if(ret != null) return ret;
        
        try {
            ret = new LoadedLibrary(loader.loadLogisimFile(toRead));
        } catch(LoadFailedException e) {
            loader.showError(e.getMessage());
            return null;
        }
        
        fileMap.put(toRead, new WeakReference(ret));
        invMap.put(ret, toRead);
        return ret;
    }
    
    public LoadedLibrary loadJarLibrary(Loader loader, File toRead, String className) {
        JarDescriptor jarDescriptor = new JarDescriptor(toRead, className);
        LoadedLibrary ret = findKnown(jarDescriptor);
        if(ret != null) return ret;

        try {
            ret = new LoadedLibrary(loader.loadJarFile(toRead, className));
        } catch(LoadFailedException e) {
            loader.showError(e.getMessage());
            return null;
        }
        
        fileMap.put(jarDescriptor, new WeakReference(ret));
        invMap.put(ret, jarDescriptor);
        return ret;
    }
    
    public void reload(Loader loader, LoadedLibrary lib) {
        Object descriptor = invMap.get(lib);
        if(descriptor == null) {
            loader.showError(StringUtil.format(Strings.get("unknownLibraryFileError"),
                    lib.getDisplayName()));
            return;
        }
        
        try {
            if(descriptor instanceof JarDescriptor) {
                JarDescriptor jarDesc = (JarDescriptor) descriptor;
                lib.setBase(loader.loadJarFile(jarDesc.file, jarDesc.className));
            } else {
                File file = (File) descriptor;
                lib.setBase(loader.loadLogisimFile(file));
            }
        } catch(LoadFailedException e) {
            loader.showError(e.getMessage());
            return;
        }
    }
    
    public Library findReference(LogisimFile file, File query) {
        for(Iterator it = file.getLibraries().iterator(); it.hasNext(); ) {
            Library lib = (Library) it.next();
            Object desc = invMap.get(lib);
            if(desc != null) {
                if(desc instanceof JarDescriptor) {
                    JarDescriptor jarDesc = (JarDescriptor) desc;
                    if(query.equals(jarDesc.file)) return lib;
                } else {
                    if(query.equals(desc)) return lib;
                }
            }
            if(lib instanceof LoadedLibrary) {
                LoadedLibrary loadedLib = (LoadedLibrary) lib;
                if(loadedLib.getBase() instanceof LogisimFile) {
                    Library ret = findReference((LogisimFile) loadedLib.getBase(), query);
                    if(ret != null) return lib;
                }
            }
        }
        return null;
    }
    
    public void fileSaved(Loader loader, File dest, File oldFile, LogisimFile file) {
        LoadedLibrary old = findKnown(oldFile);
        if(old != null) {
            old.setDirty(false);
        }
        
        LoadedLibrary lib = findKnown(dest);
        if(lib != null) {
            LogisimFile clone = file.cloneLogisimFile(loader);
            clone.setName(file.getName());
            clone.setDirty(false);
            lib.setBase(clone);
        }
    }

    public String getDescriptor(Loader loader, Library lib) {
        if(loader.getBuiltin().getLibraries().contains(lib)) {
            return desc_sep + lib.getName();
        }
        
        Object desc = invMap.get(lib);
        if(desc instanceof JarDescriptor) {
            JarDescriptor jarDesc = (JarDescriptor) desc;
            return "jar#" + toRelative(loader, jarDesc.file) + desc_sep + jarDesc.className;
        } else if(desc instanceof File) {
            File file = (File) desc;
            return "file#" + toRelative(loader, file);
        } else {
            throw new LoaderException(StringUtil.format(
                    Strings.get("fileDescriptorUnknownError"), lib.getDisplayName()));
        }
    }

    private LoadedLibrary findKnown(Object key) {
        WeakReference retLibRef = (WeakReference) fileMap.get(key);
        if(retLibRef == null) return null;
        
        LoadedLibrary retLib = (LoadedLibrary) retLibRef.get();
        if(retLib == null) {
            fileMap.remove(key);
            return null;
        }

        return retLib;
    }
    
    private String toRelative(Loader loader, File file) {
        File currentDirectory = loader.getCurrentDirectory();
        if(currentDirectory == null) {
            try {
                return file.getCanonicalPath();
            } catch(IOException e) {
                return file.toString();
            }
        }

        File fileDir = file.getParentFile();
        if(fileDir != null) {
            if(currentDirectory.equals(fileDir)) {
                return file.getName();
            } else if(currentDirectory.equals(fileDir.getParentFile())) {
                return fileDir.getName() + "/" + file.getName();
            } else if(fileDir.equals(currentDirectory.getParentFile())) {
                return "../" + file.getName();
            }
        }
        try {
            return file.getCanonicalPath();
        } catch(IOException e) {
            return file.toString();
        }
    }
}
