
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

public class config_frame extends javax.swing.JFrame {

  MicroP25RXMon parent;
  int total_bytes=0;

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public config_frame(MicroP25RXMon p) {
      initComponents();
      parent = p;
      ta.setForeground( java.awt.Color.white );
      ta.setBackground( java.awt.Color.black );
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
      //System.out.println("total: "+total_bytes);
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    private void read_configActionPerformed(java.awt.event.ActionEvent evt) {
      System.out.println("read config");
      Serial serial = parent.getSerial();
      total_bytes=0;
      if(serial!=null) {
        ta.setText("");
        parent.serial.write( new String("send_config\r\n").getBytes() );
      }
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    private void write_configActionPerformed(java.awt.event.ActionEvent evt) {
      System.out.println("write config");
    }


    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton read_config;
    private javax.swing.JTextArea ta;
    private javax.swing.JButton write_config;
}
