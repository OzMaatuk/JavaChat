
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

/**
 * @course Computers Network 
 * @Exercise 05, Chat+FTP
 * @author Oz 305181158, Avishalom 308481423
 */

/**
* Client Class with JFrame
*/
public class Client extends javax.swing.JFrame {
    
    //Parameters for client: Name, Setver IP Address, Server Port to connect.
    private String clientName, serverIP;
    private final int port = 2222;
    private int filesPort = 55000;
    private final int maxFileSize = 6022386;
    
    //Parameters for client connection: Socket with server, Writer to server, Reader from server,
    //Listener to server bt Thread, socket for files transfare.
    private Socket clientSocket;
    private Socket filesSocket;
    private BufferedReader clientReader;
    private PrintWriter clientWriter;
    private Listener listenThread;
    
    /**
     * public class filesListener extends Thread,
     * running in background and reciving a file from server by reading from the server Reader, 
     */
    public class filesListener extends Thread
    {   
        private String fileName = "";
        
        //get the file name to recieve
        public filesListener(String newFileName)
        {
            fileName = newFileName;
        }
        
        /**
        * The run method of the thread.
        * recieving file from server on filesSocket.
        * @extends Thread
        */
        @Override
        public void run() 
        {
            InputStream is = null;
            try {
                int bytesRead;
                int current = 0;
                byte [] mybytearray = new byte[maxFileSize];
                //getting input and output stream from socket
                is = filesSocket.getInputStream();
                FileOutputStream fos = new FileOutputStream("E:\\clientFolder\\" + fileName);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                //receive file reading to the byte array
                do{
                    bytesRead =  is.read(mybytearray, current, (mybytearray.length-current));
                    if (bytesRead >=0) current +=bytesRead;
                }while (bytesRead > -1);
                //write file from inputstream to path
                bos.write(mybytearray, 0, current);
                bos.flush();
                bos.close();
                fos.close();
                is.close();
                filesSocket.close();
                chatDialog.append("Done recieving file!\n");
                this.interrupt();
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    /**
     * public class Listener extends Thread,
     * running in background and listening to messages from server by reading from the server Reader,
     * when message is read, it sending it to the proper function. 
     */
    public class Listener extends Thread
    {   
        /**
        * The run method of the thread.
        * sends a "New User" message when starts, and then working while messages from server can be accepted
        * @extends Thread
        */
        @Override
        public void run() 
        {
            //sends a "New User" message to server
            clientWriter.println("newUser:" + clientName);
            clientWriter.flush();
            
            //parameters for the listener: the message itself
            String msg = "";

            //try while messages from server can be accepted
            try { 
                while ((msg = clientReader.readLine()) != null)
                {
                    //calls function to split message and handle it
                    splitAndHandle(msg); 
                    
                    //clear buffer and set text area view to the last line.
                    clientWriter.flush();
                    chatDialog.setCaretPosition(chatDialog.getDocument().getLength());
                }
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        /**
        * private void splitAndHandle
        * splits the that was accepted by the char ':'
        * read the command park of the message (data[0]) and send it to the proper function to handle
        * @param msg - - message that was accepted from server Reader
        */
        private void splitAndHandle(String msg) throws IOException
        {
            //parameters for spliting the message and puttig into array
            String[] data;
            data = msg.split(":");
                     
                     //got a connectiong
                     if (data[0].equals("connected"))
                     {
                         chatDialog.append("Connection established! \n");
                     }
                     
                     //the client name that used is already taken. cant connect and needs to change name.
                     if (data[0].equals("nameToken")) 
                     {
                        chatDialog.append("your name is already in use, please change and connect again \n");
                        disconnectButActionPerformed(null);
                     }
                     
                     //recieve a message from somebody
                     if (data[0].equals("msg")) 
                     {
                         recievedMsg(data);
                     }
                     
                     //recieve a message on a new user
                     if (data[0].equals("newUser")) 
                     {
                         addUser(data[1]);
                     }
                     
                     //recieve a message on removing user
                     if (data[0].equals("removeUser")) 
                     {
                         removeUser(data[1]);
                     }
                     
                     //recieve a message thats the server fall down
                     if (data[0].equals("serverOff")) 
                     {
                        serverOff();
                     }
                     
                    //recieve a message thats the server is going to send a file
                     if (data[0].equals("sndFile"))
                     {
                         chatDialog.append("recive file:" + data[1] + "\n");
                         recievedFile(data[1]);
                     }
                     
                     //half way on file trasfare
                     if (data[0].equals("halfFile"))
                     {
                         chatDialog.append("half of the file is been accepted, press continue for resuming download \n");
                     }
                     
                     //there is no free socket on server for ftp
                     if (data[0].equals("Busy"))
                     {
                         chatDialog.append("server if busy for file transfre, try again later \n");
                         downFileBut.setText("Download File");
                     }
                     
                     //get the port for ftp
                     if (data[0].equals("FTPort"))
                     {
                         filesPort = Integer.parseInt(data[1]);
                     }
        }
        
        /**
        * private void recievedFile
        * creating a socket for file transfer, and start file recieve thread.
        * @param msg - - message to merge and print.
        */
        private void recievedFile(String fileName) throws IOException
        {
            filesSocket = new Socket(serverIP, filesPort);
            chatDialog.append("FTP Connection starts\n");
            filesListener fileReciver = new filesListener(fileName);
            fileReciver.start();
        }
        
        /**
        * private void recievedMsg
        * merging the rest of the message, and prints it to the chat text area.
        * @param msg - - message to merge and print.
        */
        private void recievedMsg(String data[])
        {
            String newMsg="";
            for(int i =2; i<data.length; i++){
                if(i!=2)
                    newMsg+=":";
                newMsg+=data[i];
            }
            chatDialog.append(data[1] + ": " + newMsg + "\n");
        }
        
        /**
        * private void addUser(String newUser)
        * adding a new user to the users combo box, and prints proper message.
        * @param newUser - - the new user name to add.
        */
        private void addUser(String newUser)
        {
            chatDialog.append(newUser + " connected to server \n");
            usersCombo.insertItemAt(newUser, usersCombo.getItemCount()-1);
        }
        
        /**
        * private void removeUser(String rmUser)
        * remove user from the users combo box, and prints proper message.
        * @param rmUser - - the user name to remove.
        */
        private void removeUser(String rmUser)
        {
            chatDialog.append(rmUser + " disconnected from server \n");

            ComboBoxModel<String> model = usersCombo.getModel();
            List<String> arr = new LinkedList<>();
            for (int i = 0; i < model.getSize(); i++)
            {
                arr.add(model.getElementAt(i));
            }
            
            usersCombo.removeAllItems();
            
            for (int i = 0; i < arr.size(); i++)
            {
                if (arr.get(i).equals(rmUser))
                {
                    arr.remove(i);
                    break;
                }
            }
            usersCombo.setModel(new DefaultComboBoxModel(arr.toArray()));
        }
        
        /**
        * private void serverOff()
        * flushing and closing connections with server, interrupt the listener thread, and making "zero" users combo box.
        * @throws IOException
        */
        private void serverOff() throws IOException
        {
            chatDialog.append("server is off, disconnecting \n");
            clientWriter.flush();
            nameText.setEditable(true);
            connectBut.setEnabled(true);
            disconnectBut.setEnabled(false);
            usersCombo.removeAllItems();
            usersCombo.addItem("Broadcast");
            //clientReader.close();
            //clientWriter.close();
            //clientSocket.close();
            //filesSocket.close();
            clientReader = null;
            clientWriter = null;
            clientSocket = null;
            filesSocket = null;
            listenThread.stop();
        }
    }
    
    /**
     * public Client()
     * GUI JFrame default Constructor
     */
    public Client() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sendBut = new javax.swing.JButton();
        serverLabel = new javax.swing.JLabel();
        serverText = new javax.swing.JTextField();
        nameLabel = new javax.swing.JLabel();
        nameText = new javax.swing.JTextField();
        connectBut = new javax.swing.JButton();
        disconnectBut = new javax.swing.JButton();
        usersCombo = new javax.swing.JComboBox();
        chatText = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        chatDialog = new javax.swing.JTextArea();
        clearBut = new javax.swing.JButton();
        filesListBut = new javax.swing.JButton();
        downFileBut = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        sendBut.setText("Send");
        sendBut.setEnabled(false);
        sendBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButActionPerformed(evt);
            }
        });

        serverLabel.setText("Server IP:");

        serverText.setEditable(false);
        serverText.setText("localhost");

        nameLabel.setText("Name:");

        connectBut.setText("Connect");
        connectBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButActionPerformed(evt);
            }
        });

        disconnectBut.setText("Disconnect");
        disconnectBut.setEnabled(false);
        disconnectBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disconnectButActionPerformed(evt);
            }
        });

        usersCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Broadcast" }));
        usersCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usersComboActionPerformed(evt);
            }
        });

        chatDialog.setColumns(20);
        chatDialog.setRows(5);
        jScrollPane1.setViewportView(chatDialog);

        clearBut.setText("Clear");
        clearBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButActionPerformed(evt);
            }
        });

        filesListBut.setText("Files List");
        filesListBut.setEnabled(false);
        filesListBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filesListButActionPerformed(evt);
            }
        });

        downFileBut.setText("Download File");
        downFileBut.setEnabled(false);
        downFileBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downFileButActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(usersCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chatText, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(serverText, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(sendBut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(nameLabel)
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 6, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(serverLabel)
                                .addGap(39, 39, 39))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(disconnectBut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(connectBut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(nameText))
                            .addComponent(clearBut, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(filesListBut, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(downFileBut, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(serverLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(serverText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(nameLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nameText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(connectBut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(disconnectBut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearBut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filesListBut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(downFileBut)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sendBut)
                    .addComponent(usersCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chatText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
    * private void disconnectButActionPerformed
    * activate when disconnect button is clicked by user
    * sends disconnect message to server, prints disconnects message on chat text area,
    * flushing and closing connections with server, interrupt the listener thread, and making "zero" users combo box.
    */
    private void disconnectButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disconnectButActionPerformed
        // TODO add your handling code here:
        
            clientWriter.println("disconnected: " + clientName + "\n");
            clientWriter.flush();
            listenThread.stop();
            try {
                clientReader.close();
                clientSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            clientWriter.close();
            //clientWriter.flush();
            chatDialog.append("disconnecting from server... \n");
            nameText.setEditable(true);
            downFileBut.setEnabled(false);
            connectBut.setEnabled(true);
            filesListBut.setEnabled(false);
            disconnectBut.setEnabled(false);
            usersCombo.removeAllItems();
            usersCombo.addItem("Broadcast");
            clientWriter = null;
            clientReader = null;
            clientSocket = null;
    }//GEN-LAST:event_disconnectButActionPerformed
    
    /**
    * private void disconnectButActionPerformed
    * activate when connect button is clicked by user
    * gets client name and server IP from GUI
    * opening socket to server, saving writer to server, and reader from server,
    * creating the listener thread to the server, and starting it,
    * setting GUI to connection mode. 
    */
    private void connectButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButActionPerformed
        // TODO add your handling code here:
        clientName = nameText.getText();
        serverIP = serverText.getText();
        
        try 
            {
                chatDialog.append("Connecting to server... \n");
                clientSocket = new Socket(serverIP, port);
                
                InputStreamReader streamReader = new InputStreamReader(clientSocket.getInputStream());
                clientReader = new BufferedReader(streamReader);
                clientWriter = new PrintWriter(clientSocket.getOutputStream());
                
                listenThread = new Listener();
                listenThread.start();
                connectBut.setEnabled(false);
                nameText.setEditable(false);
                disconnectBut.setEnabled(true);
                connectBut.setEnabled(false);
                sendBut.setEnabled(true);
                filesListBut.setEnabled(true);
                downFileBut.setEnabled(true);
            }
        catch (Exception ex) 
            {
                chatDialog.append("Cannot Connect! Try Again. \n");
                nameText.setEditable(true);
            }
    }//GEN-LAST:event_connectButActionPerformed
    
    /**
    * private void sendButActionPerformed
    * activate when send button is clicked by user
    * getting the message that was writen by client on chat text box
    * if it not empty, send it to the server by the server writen in the proper form
    * print the message on the chat text area, and set the view to the last message.
    */
    private void sendButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButActionPerformed
        // TODO add your handling code here:
        String sendMSG = chatText.getText();
        if (!sendMSG.equals(""))
        {
            try {
                clientWriter.println("msg:" + clientName + ":" + usersCombo.getSelectedItem().toString() + ": " + sendMSG + " \n");
                clientWriter.flush(); // flushes the buffer
                chatDialog.append("you send mesage to " + usersCombo.getSelectedItem().toString() + ": " + sendMSG + "\n");
                } catch (Exception ex) {
                    chatDialog.append("Message was not sent. \n");
                }
            chatText.setText("");
            chatDialog.setCaretPosition(chatDialog.getDocument().getLength());
        }
    }//GEN-LAST:event_sendButActionPerformed
    
    /**
    * private void clearButActionPerformed
    * clear the chat text area
    */
    private void clearButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButActionPerformed
        // TODO add your handling code here:
        chatDialog.setText("");
    }//GEN-LAST:event_clearButActionPerformed
    
    /**
    * private void formWindowClosing
    * activate when X button is clicked by user
    * activating private void disconnectButActionPerformed to disconnect
    */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        disconnectButActionPerformed(null);
        //System.exit(1);
    }//GEN-LAST:event_formWindowClosing

    /**
    * private void filesListButActionPerformed
    * activate when Files List button is clicked by user
    * asking from server the list of files he got.
    */
    private void filesListButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filesListButActionPerformed
        // TODO add your handling code here:
        clientWriter.println("filesList:");
        clientWriter.flush();
        
    }//GEN-LAST:event_filesListButActionPerformed

    /**
    * private void downFileButActionPerformed
    * activate when Download File button is clicked by user
    * sends a request for a file from the server.
    */
    private void downFileButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downFileButActionPerformed
        // TODO add your handling code here:
        if (downFileBut.getText().equals("Download File"))
        {
            String fileName = chatText.getText();
            serverIP = serverText.getText();
            chatText.setText("");
            //open new connection for files transfer
            clientWriter.println("askFile:" + fileName);
            clientWriter.flush();
            downFileBut.setText("Continue Download");
        }
        else
        {
            clientWriter.println("continue:");
            clientWriter.flush();
            chatDialog.append("Approved... \n");
            downFileBut.setText("Download File");
        }
    }//GEN-LAST:event_downFileButActionPerformed

    private void usersComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usersComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_usersComboActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Client().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea chatDialog;
    private javax.swing.JTextField chatText;
    private javax.swing.JButton clearBut;
    private javax.swing.JButton connectBut;
    private javax.swing.JButton disconnectBut;
    private javax.swing.JButton downFileBut;
    private javax.swing.JButton filesListBut;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JTextField nameText;
    private javax.swing.JButton sendBut;
    private javax.swing.JLabel serverLabel;
    private javax.swing.JTextField serverText;
    private javax.swing.JComboBox usersCombo;
    // End of variables declaration//GEN-END:variables
}
