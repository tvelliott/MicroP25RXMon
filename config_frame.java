
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

public class config_frame extends javax.swing.JFrame {

  MicroP25RXMon parent;
  int total_bytes=0;
  DefaultCaret caret;

  byte[] serial_buffer = new byte[2048];
  private int packet_id=0;

  byte[] data_frame;
  int data_frame_len;
  int data_frame_off;

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public config_frame(MicroP25RXMon p) {
      initComponents();
      parent = p;
      ta.setForeground( java.awt.Color.white );
      ta.setBackground( java.awt.Color.black );

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

        jPanel1 = new javax.swing.JPanel();
        read_config = new javax.swing.JButton();
        write_config = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        ta = new javax.swing.JTextArea();

        setTitle("MicroP25RXMon Config");
        setBounds(new java.awt.Rectangle(0, 0, 640, 480));

        read_config.setText("Read Config");
        read_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                read_configActionPerformed(evt);
            }
        });
        jPanel1.add(read_config);

        write_config.setText("Write Config");
        write_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                write_configActionPerformed(evt);
            }
        });
        jPanel1.add(write_config);

        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_END);

        ta.setColumns(20);
        ta.setRows(5);
        ta.setFont(new java.awt.Font("FreeMono", 0, 18)); 
        jScrollPane1.setViewportView(ta);

        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        pack();
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public void addString(String str) {
      ta.append(str);
      ta.setCaretPosition( ta.getText().length() );
      total_bytes += str.length();
      caret.setDot(total_bytes);
      ta.setCaretColor(java.awt.Color.white);
      ta.getCaret().setVisible(true);
      ta.getCaret().setBlinkRate(250);
      //System.out.println("total: "+total_bytes);
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
      System.out.println("read config");
      total_bytes=0;
      data_frame_off=0;

      ta.setText(""); //clear all text

      //send_config causes the receiver to send BACKUP ini config information to port 6
      packet_id=0; //increment this on ack for >1 frames 
      byte[] b = new String("send_config\r\n").getBytes();
      send_frame( b, b.length, 0, 1, packet_id);   //port 1 is a \n-terminated command string
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    private void write_configActionPerformed(java.awt.event.ActionEvent evt) {
      System.out.println("write config");

      try {
        data_frame = ta.getText().getBytes();
        data_frame_len = data_frame.length; 
        packet_id=0;
        data_frame_off=0;

        int send_len = data_frame_len;
        if(send_len>512) send_len=512;

        send_frame( data_frame, send_len, data_frame_off, 2, packet_id);   //port 2 is write to mem 
      } catch(Exception e) {
        e.printStackTrace();
      }
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    void rx_ack(int rxid) {
      if(packet_id==rxid) {
        packet_id++;

        if(data_frame_len>512) data_frame_len-=512;
        else if(data_frame_len>0) data_frame_len=0;

        int send_len = data_frame_len;
        if(send_len>512) send_len=512;

        if(data_frame_len>0) data_frame_off+=512;

        if(data_frame_len>0) send_frame( data_frame, send_len, data_frame_off, 2, packet_id);   //send next write to mem frame
      }
    }


    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton read_config;
    private javax.swing.JTextArea ta;
    private javax.swing.JButton write_config;
}
