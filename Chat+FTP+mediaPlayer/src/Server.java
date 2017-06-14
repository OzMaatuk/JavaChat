
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @course Computers Network 
 * @Exercise 05, Chat+FTP
 * @author Oz 305181158, Avishalom 308481423
 */

/**
* Server Class with JFrame
*/
public class Server extends javax.swing.JFrame {

    //parameters for server: array list of clients thread, server listener thread.
   private ArrayList<ListenToClient> clientsThreadsArr = null;
   private serverListener serverTh = null;
   private ArrayList<String> filesNames = null;
   private ServerSocket filesServerSocket1 = null;
   private ServerSocket filesServerSocket2 = null;
    
   /**
    * public class fileSender
    * @extends Thread
    * a Thread class that sends a file on a new socket from server to client.
    */
    public class fileSender extends Thread	
    {
        //all requaiers parameters for client server connection by socket
       private Socket fileClientSocket;
       private String fileName = "";
       private ListenToClient clientControl = null;
       private int sSocketNum = 0;
       /**
        * public fileSender
        * fileSender constructor.
        * define all parameter for server - client connection (reader, writer)
        * @param newFileClientSocket - get the new Socket for file transfer.
        * @param newfileName - get the file name to send.
        * @param newClientControl - gets the Client thread to control connection while file transfer.
        * @throws IOException 
        */
       public fileSender(Socket newFileClientSocket, String newfileName, ListenToClient newClientControl, int ssNum) throws IOException 
       {
            try 
            {
                fileClientSocket = newFileClientSocket;
                fileName = newfileName;
                clientControl = newClientControl;
                sSocketNum = ssNum;
            }
            catch (Exception ex) 
            {
                serverDialog.append("Unexpected error... \n");
            }
       }
       
       /**
        * The run method of the thread.
        * sends file to client on the fileClientSocket,
        */
       @Override
       public void run() 
        {
            FileInputStream fis = null;
            try {
                //geting the new file from storage
                File myFile = new File ("E:\\serverCloud\\" + fileName);
                
                
                byte [] mybytearray1 = new byte [(int)myFile.length()/2];
                byte [] mybytearray2 = new byte [(int)myFile.length()/2 + (int)myFile.length()%2];
                
                //creating the in stream for reading the file
                fis = new FileInputStream(myFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                //read the file to a bytes array
                bis.read(mybytearray1, 0, mybytearray1.length);
                bis.read(mybytearray2, 0, mybytearray2.length);
                //creating the out stream for trasfaring file
                OutputStream os = fileClientSocket.getOutputStream();
                clientControl.clientWriter.println("msg:server:File on the way.");
                clientControl.clientWriter.flush();
                //sending the first half of the file
                os.write(mybytearray1, 0, mybytearray1.length);
                os.flush();
                clientControl.clientWriter.println("halfFile:");
                clientControl.clientWriter.flush();
                serverDialog.append("waiting for approve downloading \n");
                
                String msg = "";
                while ((msg = clientControl.clientReader.readLine()) != null)
                {
                    if (msg.equals("continue:"))
                    {
                        break;
                    }
                }
                                
                //sends the second half
                os.write(mybytearray2, 0, mybytearray2.length);
                os.flush();

                serverDialog.append("DONE sending file \n");
                serverDialog.setCaretPosition(serverDialog.getDocument().getLength());
                
                //close streams
                os.close();
                bis.close();
            }
            catch (FileNotFoundException ex)
            {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            catch (IOException ex)
            {
               Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
           } 
            finally
            {
               try
               {
                     //closing connection
                     fis.close();
               }
               catch (IOException ex)
               {
                   Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
               }
            }
            //stoping thread
            this.stop();
        } 
    }
   
   /**
    * public class ListenToClient
    * @extends Thread
    * a Thread class that listening to messages on the socket from server.
    */
    public class ListenToClient extends Thread	
    {
        //all requaiers parameters for client server connection by socket
       private BufferedReader clientReader;
       private Socket clientSocket;
       private PrintWriter clientWriter;
       private String clientName = "";
       
       /**
        * public ListenToClient
        * ListenToClient constructor.
        * define all parameter for server - client connection (reader, writer)
        * @param newClientSocket - get the Socket of the new client.
        * @throws IOException 
        */
       public ListenToClient(Socket newClientSocket) throws IOException 
       {
            try 
            {
                clientSocket = newClientSocket;
                clientWriter = new PrintWriter(clientSocket.getOutputStream());
                InputStreamReader isReader = new InputStreamReader(clientSocket.getInputStream());
                clientReader = new BufferedReader(isReader);
            }
            catch (Exception ex) 
            {
                serverDialog.append("Unexpected error... \n");
            }
       }
       
       /**
        * The run method of the thread.
        * working while messages from server can be accepted,
        * accepting message and sends it to the proper function to handle (by the first command in the message)
        */
       @Override
       public synchronized void run() 
       {
            //parameters for spliting the message and puttig into array
            String[] data;
            String msg = "";
            try 
        {
                while ((msg = clientReader.readLine()) != null)
                {
                    data = msg.split(":");

                    //synchronized(clientsThreadsArr)
                    {
                        if (data[0].equals("newUser"))
                        {
                            addUser(data[1], clientWriter);
                            clientName = data[1];
                            clientsThreadsArr.add(this);
                        }

                        if (data[0].equals("msg"))
                        {
                            //merging the rest of the message (all message with ':' char after the commands)
                            String newMsg = "";
                            for (int i = 3; i < data.length; i++) {
                                if (i != 3) {
                                    newMsg += ":";
                                }
                            newMsg += data[i];
                            }

                            recievedMsg(data[1], data[2], newMsg);
                        }

                        if (data[0].equals("disconnected")) 
                        {
                            userDis(data[1]);
                        }

                        if (data[0].equals("askFile"))
                        {
                            fileSender fileSenderTh = askFile(this, data[1]);
                            if (fileSenderTh != null)
                            {
                                while (fileSenderTh.isAlive())
                                {
                                    //waiting...
                                }

                                //close server socket for ftp
                                if (fileSenderTh.sSocketNum == 55000)
                                {
                                    filesServerSocket1.close();
                                    filesServerSocket1 = null;
                                }
                                else if (fileSenderTh.sSocketNum == 55001)
                                {
                                    filesServerSocket2.close();
                                    filesServerSocket2 = null;
                                }
                            }
                        }

                        if (data[0].equals("filesList"))
                        {
                            askFilesList(clientName);
                        }
                    }
                    clientWriter.flush();
                    serverDialog.setCaretPosition(serverDialog.getDocument().getLength());
                }
            } 
            catch (Exception ex)
            {
               serverDialog.append("Lost a connection with " + clientName + " on listener thread" + "\n");
               clientWriter.println("serverOff:");
                try {
                    userDis(clientName);
                } catch (IOException ex1) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
	} 
    }
    
    /**
    * private void askFilesList
    * pass a message of the files in the server
    * @param from - - name of the asker.
    */
    private void askFilesList(String from) 
    {
        String filesList = "";
        //creating the list
        File f = new File("E:\\serverCloud\\");
        filesNames = new ArrayList<String>(Arrays.asList(f.list()));
        for(String curFile : filesNames)
        {
            filesList = filesList + ", " + curFile;
        }
        
        //finding client thread and sends the list
        if (clientsThreadsArr != null)
        {
            for (ListenToClient curClient : clientsThreadsArr)
            {
                if (curClient.clientName.equals(from))
                {
                    curClient.clientWriter.println("msg:server:" + filesList);
                }
            }
        }
    }
    
    /**
    * private void askFile
    * opening socket for file transfer, waiting for connection, and starting the file sender thread.
    * @param fileName - - the file name to send.
    * @param clientControl - gets the Client thread to control connection while file transfer.
    * 
    */
    private fileSender askFile(ListenToClient clientControl, String fileName) throws FileNotFoundException, IOException 
    {
        serverDialog.append("File:" + fileName + " is been asked \n");

        //check if file exist, and creating client socket for file transfare
        boolean exist = false;
        Socket fileClientSocket = null;
        fileSender fileSenderTh = null;
        //ServerSocket myFTPserver = null;

        //find if the file is exist
        for (String curFName : filesNames)
        {
            if (curFName.equals(fileName))
            {
                exist = true;
            }
        }
        
        if (exist)
        {
            serverDialog.append("Starts sending file \n");

            try {
                //start file transfar connection - socket and thread
                //choose free socket
                if (filesServerSocket1 != null && filesServerSocket2 != null)
                {
                    clientControl.clientWriter.println("Busy:");
                    serverDialog.append("there is no free socket for file transfare \n");
                    return null;
                }
                else
                {
                    if (filesServerSocket1 == null || filesServerSocket1.isClosed())
                    {
                        clientControl.clientWriter.println("FTPort:55000");
                        clientControl.clientWriter.flush();
                        filesServerSocket1 = new ServerSocket(55000);
                        serverDialog.append("There is a new connection for file transfre on port 55000\n");
                        clientControl.clientWriter.println("sndFile:" + fileName);
                        clientControl.clientWriter.flush();
                        fileClientSocket = filesServerSocket1.accept();
                        //fileClientSocket = myFTPserver.accept();
                        fileSenderTh = new fileSender(fileClientSocket, fileName, clientControl, 55000);
                        fileSenderTh.start();
                    }
                    else
                    {
                        clientControl.clientWriter.println("FTPort:55001");
                        clientControl.clientWriter.flush();
                        filesServerSocket2 = new ServerSocket(55001);
                        serverDialog.append("There is a new connection for file transfre on port 55001\n");
                        clientControl.clientWriter.println("sndFile:" + fileName);
                        clientControl.clientWriter.flush();
                        fileClientSocket = filesServerSocket2.accept();
                        //fileClientSocket = myFTPserver.accept();
                        fileSenderTh = new fileSender(fileClientSocket, fileName, clientControl, 55001);
                        fileSenderTh.start();
                    }
                }
            } catch (IOException ex) {
                serverDialog.append("\n Error making a connection for file transfare. \n");
            }
        }
        else
        {
            clientControl.clientWriter.println("msg:server:File not found");
        }
        return fileSenderTh;
    }
    
    /**
    * private void sendBroadcast
    * first, check if the name if free,
    * add a new user to the clients threads array, and sends a new user message to all users,
    * @param userName - - name of the user who sends.
    * @param clientWriter - - the Writer thread to the new user.
    */
    private void addUser(String userName, PrintWriter clientWriter) 
    {
        //checking if the name already exist
        boolean nameToken = false;
        if (clientsThreadsArr != null)
        {
            for (ListenToClient curClient : clientsThreadsArr)
            {
                if (curClient.clientName.equals(userName))
                {
                    nameToken = true;
                }
            }
        }

        //if name is token, else add the new client to clients threads array and combobox
        if (nameToken)
        {
            clientWriter.println("nameToken:");
        }
        else
        {
            clientWriter.println("connected:");
            serverDialog.append(userName + " is connected \n");

            if (clientsThreadsArr != null)
            {
                for (ListenToClient curClient : clientsThreadsArr)
                {
                    curClient.clientWriter.println("newUser:" + userName);
                    curClient.clientWriter.flush();
                    clientWriter.println("newUser:" + curClient.clientName);
                }
            }
        }
    }
    
    /**
    * private void recievedMsg
    * pass a message to the correct user
    * @param msg - - message to send.
    * @param from - - name of the sender.
    * @param to - - name of user to accept the message
    */
    private void recievedMsg(String from, String to, String msg) 
    {
        serverDialog.append("Received message from " + from + " to " + to + ": " + msg + "\n");
        
        if (to.equals("Broadcast")) 
        {
            sendBroadcast(msg, from);
        }
        else
        {
            for (ListenToClient curClient : clientsThreadsArr)
            {
                if (curClient.clientName.equals(to))
                {
                    curClient.clientWriter.println("msg:" + from + ":" + msg + "(private message)");
                    curClient.clientWriter.flush();
                }
            }
        }
    }
    
    /**
    * private void userDis
    * remove a disconnected user from clients thread array
    * @param userName - - the user name to remove
    */
    private void userDis(String userName) throws IOException 
    {
        serverDialog.append(userName + " is disconnected \n");

        ListenToClient rmClient = null;
        //sends to all clients message about user disconnected, if it found save it.
        if (clientsThreadsArr != null)
        {
            for (ListenToClient curClient : clientsThreadsArr)
            {
                if (!curClient.clientName.equals(userName))
                {
                    curClient.clientWriter.println("removeUser:" + userName);
                    curClient.clientWriter.flush();
                }
                else
                {
                    rmClient = curClient;
                }
            }
        }
        //remove vlient from array and stops it
        clientsThreadsArr.remove(rmClient);
        rmClient.stop();
    }
    
    /**
    * private void sendBroadcast
    * sends a message to all users
    * @param msg - - message to send.
    * @param from - - name of the sender.
    */
    private void sendBroadcast(String msg, String from) 
    {
        serverDialog.append("Sending Broadcast: " + msg + "\n");
        if (clientsThreadsArr != null)
        {
            for (ListenToClient curClient : clientsThreadsArr) 
            {
                if (!curClient.clientName.equals(from))
                {
                    try 
                    {
                        PrintWriter writer = curClient.clientWriter;
                        writer.println("msg:" + from + ":" + msg);
                        writer.flush();
                    } 
                    catch (Exception ex) 
                    {
                        serverDialog.append("Error sending Broadcast. \n");
                    }
                }
            }
        }
    }
    
    /**
     * public Client()
     * GUI JFrame default Constructor
     */
    public Server() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        startBut = new javax.swing.JButton();
        stopBut = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        serverDialog = new javax.swing.JTextArea();
        clearBut = new javax.swing.JButton();
        OnUsersBut = new javax.swing.JButton();
        onFilesBut = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        startBut.setText("START");
        startBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButActionPerformed(evt);
            }
        });

        stopBut.setText("STOP");
        stopBut.setEnabled(false);
        stopBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButActionPerformed(evt);
            }
        });

        serverDialog.setEditable(false);
        serverDialog.setColumns(20);
        serverDialog.setRows(5);
        jScrollPane1.setViewportView(serverDialog);

        clearBut.setText("CLEAR");
        clearBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButActionPerformed(evt);
            }
        });

        OnUsersBut.setText("Online Users");
        OnUsersBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OnUsersButActionPerformed(evt);
            }
        });

        onFilesBut.setText("Online Files");
        onFilesBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onFilesButActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(startBut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stopBut, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearBut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(OnUsersBut, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(onFilesBut)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 41, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(clearBut)
                            .addComponent(stopBut)
                            .addComponent(startBut)
                            .addComponent(OnUsersBut)
                            .addComponent(onFilesBut))))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    /**
    * private void clearButActionPerformed
    * clear the server text area
    */
    private void clearButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButActionPerformed
        // TODO add your handling code here:
        serverDialog.setText("");
    }//GEN-LAST:event_clearButActionPerformed
    
    /**
    * private void stopButActionPerformed
    * activate when clicking start button
    * prints message about starting the server,
    * and activating the server listener thread.
    */
    private void startButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButActionPerformed
           // TODO add your handling code here:
           serverDialog.append("Server is started \n");
           serverTh = new serverListener();
           serverTh.start();
           //filesServerSocket1 = new ServerSocket(55000);
           //filesServerSocket2 = new ServerSocket(55001);
           stopBut.setEnabled(true);
           startBut.setEnabled(false);
           OnUsersButActionPerformed(null);
           onFilesButActionPerformed(null);
    }//GEN-LAST:event_startButActionPerformed

    /**
    * private void formWindowClosing
    * activate when X button is clicked by user
    * activating private void stopButActionPerformed to stop server
    */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        stopButActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    /**
    * private void stopButActionPerformed
    * activate when clicking stop button
    * prints and sends to clients message about stopping the server,
    * interrupting the server listen thread and the clients thread array
    */
    private void stopButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButActionPerformed
        // TODO add your handling code here:
        serverDialog.append("Server stopping... \n");
        //sends server off to clients
        if (clientsThreadsArr != null)
        {
            for (ListenToClient curClient : clientsThreadsArr) 
            {
                curClient.clientWriter.println("serverOff:");
                curClient.clientWriter.flush();
            }
        }
        
        try 
        {
            Thread.sleep(1000);
        } 
        catch(InterruptedException ex) {Thread.currentThread().interrupt();}
        
       try {
           serverTh.serverSocket.close();
       } catch (IOException ex) {
           Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
       }
        serverTh.stop();
        clientsThreadsArr = null;
        filesServerSocket1 = null;
        filesServerSocket2 = null;
        serverDialog.setText("");
        serverDialog.append("server stopped. \n");
        startBut.setEnabled(true);
        stopBut.setEnabled(false);
    }//GEN-LAST:event_stopButActionPerformed

    /**
    * private void OnUsersButActionPerformed
    * activate when online clients button is clicked
    * print to the server text area all names of clients from the clients array
    */
    private void OnUsersButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OnUsersButActionPerformed
        // TODO add your handling code here:
        serverDialog.append("\n Online users: \n");
        for (ListenToClient curClient : clientsThreadsArr)
        {
            serverDialog.append(curClient.clientName);
            serverDialog.append("\n");
        }
        serverDialog.append("\n");
        serverDialog.setCaretPosition(serverDialog.getDocument().getLength());
    }//GEN-LAST:event_OnUsersButActionPerformed

    private void onFilesButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onFilesButActionPerformed
        // TODO add your handling code here:
        File f = new File("E:\\serverCloud\\");
        filesNames = new ArrayList<String>(Arrays.asList(f.list()));
        serverDialog.append("\n Files on server: \n");
        for(String curFile : filesNames)
        {
            serverDialog.append(curFile);
            serverDialog.append("\n");
        }
        serverDialog.append("\n");
        serverDialog.setCaretPosition(serverDialog.getDocument().getLength());
    }//GEN-LAST:event_onFilesButActionPerformed
    
    /**
     * public class serverListener
     * @extends Thread
     * defining server socket, and clients array.
     * running in background and listening to accept messages about clients,
     * when a client is accepted, it makes a new client object for him, start it and add it to the clients array. 
     */
    public class serverListener extends Thread
    {
        ServerSocket serverSocket = null;
        
        @Override
        public void run() 
        {
            clientsThreadsArr = new ArrayList(); 
            filesNames = new ArrayList<String>();
            try 
            {
                serverSocket = new ServerSocket(2222);
                while (true) 
                {
				Socket clientSocket = serverSocket.accept();
                                ListenToClient newListenerToClient = new ListenToClient(clientSocket);
                                newListenerToClient.start();
                                serverDialog.append("There is a new connection \n");
                }
            }
            catch (Exception ex)
            {
                serverDialog.append("\n Error making a connection. \n");
            }
        }
    }
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
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Server().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton OnUsersBut;
    private javax.swing.JButton clearBut;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton onFilesBut;
    private javax.swing.JTextArea serverDialog;
    private javax.swing.JButton startBut;
    private javax.swing.JButton stopBut;
    // End of variables declaration//GEN-END:variables
}
