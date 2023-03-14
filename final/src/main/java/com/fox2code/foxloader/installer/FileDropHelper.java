package com.fox2code.foxloader.installer;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class FileDropHelper extends TransferHandler {
    private static final DataFlavor nixFileDataFlavor;

    private final JComponent component;
    private final FileDropHandler handler;
    private List<File> lastFilesWorking;

    static {
        DataFlavor nixFileDataFlavorTmp;
        try {
            nixFileDataFlavorTmp = new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException e) {
            nixFileDataFlavorTmp = null;
        }
        nixFileDataFlavor = nixFileDataFlavorTmp;
    }

    public FileDropHelper(JComponent component, FileDropHandler handler) {
        this.component = component;
        this.handler = handler;
    }

    public void enableDragAndDrop() {
        if (!this.isDragAndDropEnabled()) {
            this.component.setTransferHandler(this);
        }
    }

    public void disableDragAndDrop() {
        if (this.isDragAndDropEnabled()) {
            this.component.setTransferHandler(null);
            this.lastFilesWorking = null;
        }
    }

    public boolean isDragAndDropEnabled() {
        return this.component.getTransferHandler() == this;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        List<File> files = getFiles(support.getTransferable());
        return files != null && handler.areFilesAcceptable(files);
    }

    @Override
    public boolean importData(TransferSupport support) {
        List<File> files = getFiles(support.getTransferable());
        return files != null && handler.acceptFiles(files);
    }

    @SuppressWarnings("unchecked")
    private List<File> getFiles(Transferable transferable) {
        List<File> files = null;
        for (DataFlavor flavor : transferable.getTransferDataFlavors()) {
            if (flavor.isFlavorJavaFileListType()) {
                try {
                    files = (List<File>) transferable.getTransferData(flavor);
                    if (!files.isEmpty()) break;
                } catch (InvalidDnDOperationException e) {
                    if ("No drop current".equals(e.getMessage()))
                        return lastFilesWorking;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        if ((files == null || files.isEmpty()) &&
                transferable.isDataFlavorSupported(nixFileDataFlavor)) {
            String uriList;
            try {
                uriList = (String) transferable.getTransferData(nixFileDataFlavor);
            } catch (InvalidDnDOperationException e) {
                if ("No drop current".equals(e.getMessage()))
                    return lastFilesWorking;
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            String[] elements = uriList.split("\n");
            files = new ArrayList<>(elements.length);
            for (String element : elements) {
                element = element.trim();
                if (element.isEmpty() || element.startsWith("#")) {
                    continue;
                }
                URI uri = URI.create(element);
                if (!"file".equals(uri.getScheme())) {
                    files = null;
                    break;
                }
                files.add(new File(uri));
            }
        }
        if (files == null ||
                files.isEmpty()) {
            lastFilesWorking = null;
            return null;
        }
        lastFilesWorking = files;
        return files;
    }

    public interface FileDropHandler {
        boolean areFilesAcceptable(List<File> fileList);

        boolean acceptFiles(List<File> fileList);
    }
}
