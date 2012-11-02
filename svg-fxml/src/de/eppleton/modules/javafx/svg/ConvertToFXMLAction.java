/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.eppleton.modules.javafx.svg;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.ErrorManager;
import org.openide.loaders.DataObject;

import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.util.TaskListener;

@ActionID(category = "File",
id = "de.eppleton.modules.javafx.svg.ConvertToFXMLAction")
@ActionRegistration(displayName = "#CTL_ConvertToFXMLAction")
@ActionReferences({
    @ActionReference(path = "Loaders/text/svg+xml/Actions", position = 160, separatorBefore = 150),
    @ActionReference(path = "Editors/text/svg+xml/Popup", position = 190)
})
@Messages("CTL_ConvertToFXMLAction=Convert to FXML...")
public final class ConvertToFXMLAction implements ActionListener {

    private final DataObject context;
    private final static RequestProcessor RP = new RequestProcessor("SVG to FXML Conversion Task", 1, true);

    public ConvertToFXMLAction(DataObject context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    saveBeforeTransformation(context);
                    FileObject parent = context.getPrimaryFile().getParent();
                    FileObject transformed = null;
                    // 100 attempts should be enough 
                    try {
                        String findFreeFileName = FileUtil.
                                findFreeFileName(parent, context.
                                getPrimaryFile().getName(), "fxml");
                        transformed = parent.
                                createData(findFreeFileName, "fxml");
                    } catch (IOException ex) {
                        try { // try again for the rare coinncidence of someone stealing our filename
                            String findFreeFileName = FileUtil.
                                    findFreeFileName(parent, context.
                                    getPrimaryFile().getName(), "fxml");
                            transformed = parent.
                                    createData(findFreeFileName, "fxml");
                        } catch (IOException ex1) { // give up
                            Exceptions.printStackTrace(ex1);
                        }
                    }
                    if (transformed == null) {
                        return;
                    }

                    Source xmlSource = new StreamSource(context.getPrimaryFile().
                            getInputStream());

                    Source xsltSource = new StreamSource(ConvertToFXMLAction.class.
                            getResourceAsStream("svg2fxml.xsl"));

                    TransformerFactory transFact =
                            TransformerFactory.newInstance();
//transFact.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
                    try {
	                Field _isNotSecureProcessing = transFact.getClass().getDeclaredField("_isNotSecureProcessing");
	                _isNotSecureProcessing.setAccessible(true);
	                _isNotSecureProcessing.set(transFact, Boolean.TRUE);
	            } catch (Exception x) {
	            }
                    Transformer trans = transFact.newTransformer(xsltSource);
                    OutputStream outputStream = transformed.getOutputStream();
                    trans.transform(xmlSource, new StreamResult(outputStream));
                    outputStream.flush();
                    outputStream.close();
                    final Node node = DataObject.find(transformed).
                            getNodeDelegate();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("now I'll open " + node.
                                    getDisplayName());
                            try {
                                callAction(node.getPreferredAction(),node,new ActionEvent(node, ActionEvent.ACTION_PERFORMED, "")); // NOI18N
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (TransformerException ex) {
                    Exceptions.printStackTrace(ex);
                }

            }
        };

        final RequestProcessor.Task theTask = RequestProcessor.getDefault().
                create(runnable);

        final ProgressHandle ph = ProgressHandleFactory.
                createHandle("Converting to FXML...", theTask);
        theTask.addTaskListener(new TaskListener() {
            public void taskFinished(org.openide.util.Task task) {
                //make sure that we get rid of the ProgressHandle
                //when the task is finished
                ph.finish();
            }
        });

        //start the progresshandle the progress UI will show 500s after
        ph.start();

        //this actually start the task
        theTask.schedule(0);

    }

    private void saveBeforeTransformation(DataObject dObject) {
        if (dObject.isModified()) {
            SaveCookie save;
            save = (SaveCookie) dObject.getCookie(SaveCookie.class);
            if (save != null) {
                try {
                    save.save();
                } catch (IOException ex) {
                    ErrorManager.getDefault().
                            notify(ErrorManager.INFORMATIONAL, ex);
                }
            }
        }
         
    }
      private static void callAction(Action a, Node node, ActionEvent actionEvent) {
        if (a instanceof ContextAwareAction) {
            a = ((ContextAwareAction)a).createContextAwareInstance(node.getLookup());
        }
        if (a == null) {
            return;
        }
        a.actionPerformed(actionEvent);
    }
}
