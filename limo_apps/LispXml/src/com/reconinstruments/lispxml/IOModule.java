package com.reconinstruments.lispxml;
import android.app.Instrumentation;
import android.util.Log;
import java.io.*;
import java.util.*; 
import java.util.zip.*; 
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IOModule extends Module {
    public static final String TAG = "IOModule";
    private Instrumentation mInstru = new Instrumentation();
    public IOModule (LispXmlParser lp) {
	super(lp);
    }
    @Override
    public String getNamespace() {
	return "com.reconinstruments.lispxml.module.io";
    }
    @Override
    public Element exec (Element el, Document doc) throws Exception{
	mPrefix = el.getPrefix();
	String command = el.getLocalName();
	if (command.equals("log")) {
	    return log(el.getChildNodes(),doc);
	}
	if (command.equals("rm")) {
	    return rm(el.getChildNodes(),doc);
	}
	if (command.equals("cp")) {
	    return cp(el.getChildNodes(),doc);
	}
	if (command.equals("print")) {
	    return print(el.getChildNodes(),doc);
	}
	if (command.equals("ls")) {
	    return ls(el.getChildNodes(),doc);
	}
	if (command.equals("unzip")) {
	    return unzip(el.getChildNodes(),doc);
	}
	if (command.equals("write-to-file")) {
	    return writeToFile(el.getChildNodes(),doc,false); // not b64
	}
	if (command.equals("write-b64-to-file")) {
	    return writeToFile(el.getChildNodes(),doc,true); // isb64
	}
	if (command.equals("input-key")) {
	    return inputKey(el.getChildNodes(),doc);
	}
	if (command.equals("input-string")) {
	    return inputString(el.getChildNodes(),doc);
	}
	return super.exec(el,doc);
    }
    private Element log(NodeList nl, Document doc) throws Exception {
	Element el = Module.getElement(0,nl);
	el = mOwner.exec(el,doc);
	if (nodeis(coreModule.NS, "s",el)) {
	    Log.v("LispXmlParser", el.getTextContent());
	}
	else {
	    throw new Exception("Bad log argument");
	}
	return doc.createElement("nil");
    }

    private Element rm(NodeList nl, Document doc) throws Exception {
	Element el = Module.getElement(0,nl);
	el = mOwner.exec(el,doc);
	if (nodeis(coreModule.NS, "s",el)) {
	    File f = new File(el.getTextContent());
	    deleteRecursive(f);
	}
	else {
	    throw new Exception("Invalid argument type for rm. Expected type <s>");
	}
	return doc.createElement("nil");
    }
    private Element print(NodeList nl, Document doc) throws Exception {
	Element el = Module.getElement(0,nl);
	el = mOwner.exec(el,doc);
	mOwner.mResponse.appendChild(el);
	return doc.createElement("nil");
    }
    private Element cp(NodeList nl, Document doc)  throws Exception {
	boolean blind = false;

	try {
	    Element elSrc = mOwner.exec(Module.getElement(0,nl),doc);
	    Element elDst = mOwner.exec(Module.getElement(1,nl),doc);
	    Element elBlind = Module.getElement(2,nl);
	    if (elBlind == null) {
		blind = false;
		//Log.v(TAG,"Blind false");
	    }
	    else {
		elBlind = mOwner.exec(elBlind,doc);
		if (nodeis(coreModule.NS,"nil",elBlind)) {
		    blind = false;
		    //Log.v(TAG,"Blind false");
		} else {
		    blind = true;
		    //Log.v(TAG,"Blind dtruefalse");
		}
	    }
	    if (nodeis(coreModule.NS,"s",elSrc) &&
		nodeis(coreModule.NS,"s",elDst)) {
		File src = new File(elSrc.getTextContent());
		File dst = new File(elDst.getTextContent());
		dst.getParentFile().mkdirs();
		if (!src.isDirectory()) { // file to file or directory
		    FileUtils.copyFile(src,dst);
		}
		else if (src.isDirectory() &&
			 (dst.isDirectory() || !dst.exists())) { // directory to directory
		    FileUtils.copyDirectory(src,dst);
		}
		else {		// directory to file: bad
		    if (!blind) throw new Exception("Can't copy directory to file");
		    return doc.createElement("nil");
		}
		return doc.createElement("true");
	    }
	    else {
		if (!blind) new Exception("Bad cp argument");
	    }
	}
	catch (IOException e) {
	    if (!blind) throw e;
	    return doc.createElement("nil");
	}
	return doc.createElement("true");
    }

    private void deleteRecursive(File fileOrDirectory) {
	if (fileOrDirectory.isDirectory()) {
	    for (File child : fileOrDirectory.listFiles()) {
		deleteRecursive(child);
	    }
	}
	fileOrDirectory.delete();
    }

    private Element ls(NodeList nl, Document doc) throws Exception {
	Element el = mOwner.exec(Module.getElement(0,nl),doc);
	if (!nodeis(coreModule.NS,"s",el)) {
	    throw new Exception("Invalid argument for ls. Expected type <s>");
	}
	Element result = doc.createElement("list");
	File dir = new File(el.getTextContent());
	if (!dir.exists()) {
	    return doc.createElement("nil");
	}
	String ending;
	for (final File fileEntry : dir.listFiles()) {
	    Element s = doc.createElement("s");
	    ending = fileEntry.isDirectory()? "/":"";
	    s.appendChild(doc.createTextNode(fileEntry.getName()+ending));
	    result.appendChild(s);
	}
	return result;
    }

    private Element unzip(NodeList nl, Document doc) throws Exception {
	Element elSrc = mOwner.exec(Module.getElement(0,nl),doc);
	Element elDest = mOwner.exec(Module.getElement(1,nl),doc);
	if (!(nodeis(coreModule.NS,"s",elSrc) &&
	      nodeis(coreModule.NS,"s",elDest))) {
	    throw new Exception ("Invalid argument for unzip. ");
	}
	String zipPath = elSrc.getTextContent();
	String dest = elDest.getTextContent();
	unzip(zipPath, dest);
	return doc.createElement("nil");
    }

    private Element writeToFile(NodeList nl,Document doc,boolean isB64) throws Exception {
	Element elSrc = mOwner.exec(Module.getElement(0,nl),doc);
	Element elDest = mOwner.exec(Module.getElement(1,nl),doc);
	Element elAppend = Module.getElement(2,nl);
	if (elAppend == null) elAppend = doc.createElement("nil");
	else elAppend = mOwner.exec(elAppend,doc);
	boolean shouldAppend = false;
	if (!nodeis(coreModule.NS,"nil",elAppend)) {
	    shouldAppend = true;
	}
	if (!(nodeis(coreModule.NS,"s",elDest) &&
	      nodeis(coreModule.NS,"s",elSrc))) {
	    throw new Exception ("Invalid argument for write to file. ");
	}
	String dest = elDest.getTextContent();
	LispXmlService.writeToFile(elSrc.getTextContent(),
					 new File(dest),
					 shouldAppend,
					 isB64);
	return doc.createElement("nil");
    }


    private void unzip(String zipPath, String dest) throws Exception {
	InputStream is;
	ZipInputStream zis;
	is = new FileInputStream(zipPath);
	zis = new ZipInputStream(new BufferedInputStream(is));          
	ZipEntry ze;

	while((ze = zis.getNextEntry()) != null) {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte[] buffer = new byte[1024];
	    int count;
	    String filename = dest+"/"+ze.getName();
	    
	    File file = new File(filename);
	    if (filename.endsWith("/")) {
		file.mkdirs();
		continue;
	    }
	    
	    File parent = file.getParentFile();
	    if (parent != null) {
		parent.mkdirs();
	    }
	    FileOutputStream fout = new FileOutputStream(filename);

	    // reading and writing
	    while((count = zis.read(buffer)) != -1) {
		baos.write(buffer, 0, count);
		byte[] bytes = baos.toByteArray();
		fout.write(bytes);             
		baos.reset();
	    }
	    fout.close();               
	    zis.closeEntry();
	}
	zis.close();
    }
    private Element inputKey(NodeList nl, Document doc) throws Exception {
	Element el = mOwner.exec(Module.getElement(0,nl),doc);
	if (nodeis(MathModule.NS,"i",el)) {
	    mInstru.sendKeyDownUpSync(MathModule.element2int(el));
	}
	else {
	    throw new Exception("Bad input-key argument");
	}
	return doc.createElement("nil");
    }
    private Element inputString(NodeList nl, Document doc) throws Exception {
	Element el = mOwner.exec(Module.getElement(0,nl),doc);
	if (nodeis(coreModule.NS,"s",el)) {
	    mInstru.sendStringSync(el.getTextContent());
	}
	else {
	    throw new Exception("Bad input-string argument");
	}
	return doc.createElement("nil");
    }

}
