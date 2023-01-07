
//MIT License
//
//Copyright (c) 2022 tvelliott and goldmyne99
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

import processing.serial.*;
import processing.core.*;
import javax.swing.text.*; 
import javax.swing.*; 
import java.io.File; 
import java.io.InputStream; 
import java.io.BufferedReader; 
import java.io.OutputStream; 

public class config_frame extends javax.swing.JFrame {

  MicroP25RXMon parent;
  int total_bytes=0;
  DefaultCaret caret;

  byte[] serial_buffer = new byte[2048];
  private int packet_id=0;

  byte[] data_frame;
  int bytes_to_send;
  int data_frame_len;
  int data_frame_off;

  final JFileChooser fc;
  public int busy=0;
  private int ack_time=0;

  public int read_timeout=0;

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public config_frame(MicroP25RXMon p) {
      initComponents();
      parent = p;
      ta.setForeground( java.awt.Color.white );
      ta.setBackground( java.awt.Color.black );

      fc = new JFileChooser();

      caret = (DefaultCaret) ta.getCaret();

      caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
      ta.setCaretColor(java.awt.Color.white);
      ta.getCaret().setVisible(true);
      ta.getCaret().setBlinkRate(250);


      try {
        this.setIconImage(new javax.swing.ImageIcon(getClass().getResource("data/console.png")).getImage()); 
      } catch(Exception e) {
        //e.printStackTrace();
      }
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        ta = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        status = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        read_config = new javax.swing.JButton();
        write_config = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        loadfile = new javax.swing.JMenuItem();
        savefile = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        setTitle("MicroP25RXMon");

        ta.setBackground(new java.awt.Color(0, 0, 0));
        ta.setColumns(20);
        ta.setFont(new java.awt.Font("FreeMono", 0, 18)); // NOI18N
        ta.setForeground(new java.awt.Color(255, 255, 255));
        ta.setRows(5);
        jScrollPane1.setViewportView(ta);

        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        status.setText("STATUS:");
        jPanel3.add(status);

        jPanel2.add(jPanel3);

        read_config.setText("Read Config From Device");
        read_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                read_configActionPerformed(evt);
            }
        });
        jPanel1.add(read_config);

        write_config.setText("Write Config To Device");
        write_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                write_configActionPerformed(evt);
            }
        });
        jPanel1.add(write_config);

        jPanel2.add(jPanel1);

        getContentPane().add(jPanel2, java.awt.BorderLayout.SOUTH);

        jMenu1.setText("File");

        loadfile.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.ALT_MASK));
        loadfile.setText("Load Config File");
        loadfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadfileActionPerformed(evt);
            }
        });
        jMenu1.add(loadfile);

        savefile.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK));
        savefile.setText("Save Config File");
        savefile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                savefileActionPerformed(evt);
            }
        });
        jMenu1.add(savefile);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        pack();
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public void addString(String str) {
      read_timeout=200;

      ta.append(str);
      ta.setCaretPosition( ta.getText().length() );
      total_bytes += str.length();
      caret.setDot(total_bytes);
      ta.setCaretColor(java.awt.Color.white);
      ta.getCaret().setVisible(true);
      ta.getCaret().setBlinkRate(250);

      String s = "read: "+total_bytes+" bytes";
      System.out.println(s);
      setStatus(s);
    }

    ///////////////////////////////////////////////////////////////////////
    // port1 :  \n-terminated command string
    ///////////////////////////////////////////////////////////////////////
    private void send_frame( byte[] b, int len, int off, int port, int tx_packet_id) {

      Serial serial = parent.getSerial();
      if(serial==null) return;

      if(len > 2040) len=2040;
      if(len < 0) return; 

      serial_buffer = new byte[ len + 12];

      //add sync word
      serial_buffer[0]=(byte) 0xf7; //sync1
      serial_buffer[1]=(byte) 0xc5; //sync2
      serial_buffer[2]=(byte) 0x17; //sync3
      serial_buffer[3]=(byte) 0x29; //sync4
      serial_buffer[4]=(byte) 0x41; //sync5
      serial_buffer[5]= (byte) (port&0xff);   //PORT
      serial_buffer[6]= (byte) ((len>>8)&0xff); //frame len (16-bit)
      serial_buffer[7]= (byte) (len&0xff); 
      serial_buffer[8]= (byte) ((tx_packet_id>>24)&0xff);   //32-bit packet_id
      serial_buffer[9]= (byte) ((tx_packet_id>>16)&0xff);   
      serial_buffer[10]= (byte) ((tx_packet_id>>8)&0xff);   
      serial_buffer[11]= (byte) (tx_packet_id&0xff);   

      for(int i=0;i<len;i++) {
        serial_buffer[12+i] = b[i+off]; //frame data
      }

      if(len > 0) {
        parent.serial.write(serial_buffer);
      }

    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    private void read_configActionPerformed(java.awt.event.ActionEvent evt) {
      if(busy!=0) return;

      System.out.println("read config");
      total_bytes=0;
      data_frame_off=0;

      ta.setText(""); //clear all text

      //send_config causes the receiver to send BACKUP ini config information to port 6
      packet_id=0; //increment this on ack for >1 frames 
      byte[] b = new String("send_config\r\n").getBytes();
      send_frame( b, b.length, 0, 1, packet_id);   //port 1 is a \n-terminated command string

      busy=1;
      read_timeout=200;
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    private void write_configActionPerformed(java.awt.event.ActionEvent evt) {
      if(busy!=0) return;

      System.out.println("write config");

      try {
        data_frame = ta.getText().getBytes();
        data_frame_len = data_frame.length; 
        bytes_to_send=data_frame_len;
        packet_id=0;
        data_frame_off=0;

        int send_len = data_frame_len;
        if(send_len>512) send_len=512;

        send_frame( data_frame, send_len, data_frame_off, 2, packet_id);   //port 2 is write to mem 
        ack_time=5;

      } catch(Exception e) {
        e.printStackTrace();
      }
      busy=1;
    }
    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public void config_tick() {
      if( ack_time>0 ) {
        ack_time--;
        if(ack_time==0) {
          int send_len = data_frame_len;
          if(send_len>512) send_len=512;
          //send_frame timeout, retry
          send_frame( data_frame, send_len, data_frame_off, 2, packet_id);   //port 2 is write to mem 
          ack_time=5;
        }
      }

      if( read_timeout>0) {
        read_timeout--;
        if(read_timeout==0) {
          busy=0;
        }
      }
    }
    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    void rx_ack(int rxid) {
      if(packet_id==rxid) {
        packet_id++;
        ack_time=5;

        int tot = data_frame_len;

        if(data_frame_len>512) {
          data_frame_len-=512;
          tot=0;
        }
        else if(data_frame_len>0) {
          data_frame_len=0;
        }

        int send_len = data_frame_len;
        if(send_len>512) send_len=512;

        if(data_frame_len>0) data_frame_off+=512;


        String str = " wrote "+(tot+data_frame_off)+" bytes";
        parent.print(str);

        setStatus(str);


        if(data_frame_len>0) {
          send_frame( data_frame, send_len, data_frame_off, 2, packet_id);   //send next write to mem frame
        }
        else {
          if( (tot + data_frame_off) == bytes_to_send ) {
            parent.print("\r\nsent configuration ok");
            setStatus("Send Config With No Errors. Please Wait For Restore / Re-boot.");
            busy=0;
            ack_time=0;
          }
        }
      }
    }

    ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    public void setStatus(String s) {
        status.setText("STATUS: "+s);
    }
    ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    private void savefileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_savefileActionPerformed
      int returnVal = fc.showDialog(this,"SAVE");
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        try {
          File file = fc.getSelectedFile();
          java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "rw");
          raf.writeBytes(ta.getText());
          raf.close();
        } catch(Exception e) {
          e.printStackTrace();
        }

      }
    }//GEN-LAST:event_savefileActionPerformed

    ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    private void loadfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadfileActionPerformed
      int returnVal = fc.showDialog(this,"LOAD");
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        try {
          File file = fc.getSelectedFile();
          java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
          byte[] b = new byte[(int) file.length()];
          raf.readFully(b);
          ta.setText( new String(b) );
          raf.close();
        } catch(Exception e) {
          e.printStackTrace();
        }

      }
    }//GEN-LAST:event_loadfileActionPerformed

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////

    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JMenuItem loadfile;
    private javax.swing.JButton read_config;
    private javax.swing.JMenuItem savefile;
    private javax.swing.JLabel status;
    private javax.swing.JTextArea ta;
    private javax.swing.JButton write_config;
}
