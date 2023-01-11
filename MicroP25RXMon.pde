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
int app_ver = 2023011101;

import processing.serial.*;
import java.nio.*;


//change these lines if you need to. 1=enable, 0=disable
int have_gpu=1;
int showText = 1;
int do_meta_output=0;
int do_draw_iq=0;
int showTG_history=1;

//int serial_port = 0; //port is auto detected
int serial_baud_rate = 480000000;

Serial serial=null;

byte[] in_buffer;
byte[] buf;
int buf_idx;
int buf_len;
int total_bytes;
int rx_state=0;
int port;

float[] iq_samples;
int iq_idx;


int frame_cnt=0;
int metamod=0;
int serial_packet_count=0;
int found_port=0;

String[] ports;
int port_idx=0;
int port_to=0;
String port_name;
int avail=0;

color col1;
color col2;
color col3;
color col4;
color col5;
color col6;
color col7;
color col8;
color col9;
color col10;
color col11;
color col12;
color col13;
color col14;
color col_menu_fg;
color col_menu_bg;
color col_def_bg;
color col_def_fg;
color col_def_led1_on;
color col_def_led2_on;
color col_def_led1_off;
color col_def_led2_off;
color col_def_indicator;
color col_def_const;

String dmod = "";
audio aud;

int button_press=0;

int did_draw_config=0;

config_frame config=null;

private volatile int out_of_seq;
private volatile int prev_packet_id;
private volatile int packet_id;

String TG1="";
String TG2="";
String TG3="";
String TG4="";
String TG5="";
String TG_old="";
String TG_active="";
///////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////
void setup()
{
  size(1024, 512);
  background(0); //black
  buf = new byte[4096];
  buf_idx=0;

  iq_samples = new float[1024];
  
  // Print a list of the available serial ports
  println("-----------------------");
  println("Available Serial Ports:");
  println("-----------------------");
  printArray(Serial.list());
  println("-----------------------");
 
  aud = new audio();

  config = new config_frame(this);
  config.setSize(640,480);

}
///////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////
public Serial getSerial() {
  return serial;
}
///////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////
void draw()
{

  if( found_port==0 && serial_packet_count > 0) {
    found_port=1;
    println("found port "+port_name);
  }
  if( found_port==1 && port_to==0) {
    found_port=0;
    serial_packet_count=0;
    println("port not responding "+port_name);
  }

  if(port_to>0) port_to--;

  if(found_port==0 && port_to==0) {
    ports = Serial.list();
    if(ports!=null) { 
      serial_packet_count=0;
      if(port_idx>=ports.length) port_idx=0;

      port_name = ports[port_idx++];
      println("trying port "+port_name);


      try {
        if(serial!=null) serial.stop();
        if(serial!=null) serial.clear();
      } catch(Exception e) {
        e.printStackTrace();
        found_port=0;
      }

      try {
        serial = new Serial(this, port_name, serial_baud_rate);
        port_to=150;
        found_port=0;
      } catch(Exception e) {
        e.printStackTrace();
        found_port=0;
      }

    }

  }


  //process incoming serial data
  if(serial!=null) avail = serial.available();
  if ( avail > 128) {
    in_buffer = serial.readBytes();
    //serial.readBytes(in_buffer);
    if(in_buffer!=null) {
      total_bytes += avail;
      //println(total_bytes+" bytes");
      for(int i=0;i<in_buffer.length;i++) {
        process_buffer(in_buffer[i]);
      }
    }
  }
  
  aud.audio_tick();
  config.config_tick();
  
  if(aud.audio_active==0) {
   stroke(0,0,0);
   fill(0,0,0);
   rect(0,0,512,128); //blank to background
  }

  if(mousePressed) {
    //rect(25,475,105,25);
    if(mouseX>25 && mouseY>475 && mouseX < 130 && mouseY<500) {
      if(button_press==0) {
        print("\r\nconfig button pressed"); 
        button_press=1;
        //call config here
        config.setVisible(true);
      }
    }
  }
  else { 
    button_press=0;
  }
} 

///////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////
void process_buffer(byte b) {
 
   int  val = (int) b;
   if(val<0) {  //convert 8-bit signed to 32-bit signed
     val *= -1;
     val = 256-val;
   }
  //println("val"+val);
  if( rx_state == 0 && val == 0xf7 ) {  //sync to incoming data stream
    rx_state++;
    buf_idx = 0;
    buf[buf_idx++] = (byte) val;
  } 
  else if( rx_state == 1 && val == 0xc5 ) { //sync word
    rx_state++;
    
    port_to=150;
  }
  else if( rx_state == 2 && val == 0x17 ) { //sync word
    rx_state++;
    
  }
  else if( rx_state == 3 && val == 0x29 ) { //sync word
    rx_state++;
    
  }
  else if( rx_state == 4 && val == 0x41 ) { //sync word
    rx_state++;
    
  }
  else if( rx_state == 5 ) { //get port
    port = (int) val; 
    rx_state++;
    buf_idx=0;
    
  } else if( rx_state == 6 ) { //get upper 8bits len
    buf_len = (int) ((int) val<<8);
    rx_state++;
  } else if( rx_state == 7 ) { //get lower 8bits len
    buf_len |= (int) ((int) val);
    if(buf_len > 2048) rx_state=0;
      else rx_state++;
  }
  else if( rx_state == 8 ) { 
    packet_id = (int) ((int) val<<24);
    rx_state++;
  }
  else if( rx_state == 9 ) { 
    packet_id |= (int) ((int) val<<16);
    rx_state++;
  }
  else if( rx_state == 10 ) { 
    packet_id |= (int) ((int) val<<8);
    rx_state++;
  }
  else if( rx_state == 11 ) { 
    packet_id |= (int) ((int) val);
    rx_state++;

    if( packet_id==0 ) {
      out_of_seq=0;
    }
    else if(port==6 && packet_id!=prev_packet_id+1) {
      print("\r\ndetected out of sequence packet_id "+packet_id+" "+prev_packet_id+1);
      out_of_seq++;
    }
    prev_packet_id=packet_id;
  }
  else if(rx_state==12) {      //start collection len bytes

    buf[buf_idx++]=(byte) val;;

    if(buf_len==0  || buf_idx == buf_len) {    //handle data

      switch(port) {
        case  1 :
          if(have_gpu>0 && do_draw_iq>0) {
            draw_iq(buf,buf_len);
            serial_packet_count++;
            port_to=150;
            did_draw_config=0;
          }
        break;
        case  2 :
          if(have_gpu>0) draw_audio(buf,buf_len);
          aud.play_audio(buf,buf_len);
          serial_packet_count++;
          port_to=150;
        break;
        case  3 :
          print( new String(buf,0,buf_len) );
          serial_packet_count++;
          port_to=150;
          did_draw_config=0;
        break;
        case  4 :
          if(have_gpu>0) draw_constellation(buf,buf_len);
          serial_packet_count++;
          port_to=150;
          
          if(did_draw_config==0) {
            //draw config button
            fill(col1);
            stroke(col1);
            rect(25,475,105,25);
            fill(0,0,0);
            stroke(0,0,0);
            textSize(25);
            text("CONFIG",35,495);  
            textSize(15);
            
            fill(0,0,0);
            stroke(0,0,0);
            rect(120,475,160,25);
            fill(255,255,255);
            stroke(255,255,255);
            text("VER "+app_ver, 150,495);
            did_draw_config=1;
          }
        break;
        case  5 :
          try {
            handle_metainfo(buf,buf_len);
          } catch(Exception e) {
            e.printStackTrace();
          }
          serial_packet_count++;
          port_to=150;
        break;
        case  6 :
          //print( new String(buf,0,buf_len) );
          config.addString( new String(buf,0,buf_len) );
          serial_packet_count++;
          port_to=150;
          if(buf_len<512) {
            //print("\r\nlast packet received");
            if( out_of_seq>0) print( String.format("\r\n%d out of seq packets detected", out_of_seq) );
            else if( out_of_seq==0) print( String.format("\r\nno errors detected", out_of_seq) );
            if(out_of_seq==0 && buf_len<512) {
              config.setStatus("Read Finished With No Errors");
              config.busy=0;
              config.read_timeout=0;
            }
            else if(out_of_seq>0 && buf_len<512) {
              config.setStatus("Errors detect in read. Try Again");
              config.busy=0;
              config.read_timeout=0;
            }
          }
        break;

        case  7 :
          if( buf_len==0 ) {  //ACK has no data
            //print("\r\nACK "+packet_id);
            config.rx_ack(packet_id);
            port_to=150;
          }
        break;


        default :
          //do nothing, unknown port
        break;
      }

      rx_state=0; //get the next packet
        

    }
  }
  else {
    //println("rx_state "+rx_state+" val "+val);
    rx_state=0;
  }
   

}


///////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////
void draw_audio(byte[] b, int len) {
 ByteBuffer bb = ByteBuffer.wrap(b);
 bb.order(ByteOrder.LITTLE_ENDIAN);
 //println("audio len: "+len/2);
 short samples[] = new short[256];
 
 for(int i=0;i<len/2;i++) {
   samples[i] = (short) bb.getShort();
   samples[i] /= 256;
   if( samples[i]>128 ) samples[i]=128;
   if( samples[i]<-128 ) samples[i]=-128;
 }
 

 stroke(0,0,0);
 fill(0,0,0);
 rect(0,0,512,128); //blank to background

  fill(col_def_const);
  stroke(col_def_const);
  
 
  //draw the audio
 int step=2;
 for(int i=0;i<len/2;i++) {
   if(i>0) line(i*step,64+samples[i-1], (i*step+1), 64+samples[i]);
 }
}
///////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////
void draw_constellation(byte[] b, int len) {
 ByteBuffer bb = ByteBuffer.wrap(b);
 bb.order(ByteOrder.LITTLE_ENDIAN);

 //println("const len: "+len/2);

 float samples[] = new float[len/4];
 
 for(int i=0;i<len/4;i++) {
   samples[i] = bb.getFloat();
 }
 

 stroke(0,0,0);
 fill(0,0,0);
 rect(0,128,512,128); //blank to background

 stroke(col_def_const);
 fill(col_def_const);

  //draw the constellation
  int idx=0;
  for(int i=0;i<len/8;i++) {
    float xx = samples[idx++]/512.0f;
    float yy = samples[idx++]/512.0f;
    circle(150+xx,192+yy,1);
  }
 
}
///////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////
void draw_iq(byte[] b, int len) {
 ByteBuffer bb = ByteBuffer.wrap(b);
 bb.order(ByteOrder.LITTLE_ENDIAN);

 for(int i=0;i<len/4;i++) {
   iq_samples[iq_idx] = bb.getFloat();
   if(++iq_idx==1024) break;
 }

 if(iq_idx<1024) return;
 iq_idx=0;



 //println("iq len: "+len/2);

 int do_normalize=1;
 if( do_normalize>0 ) {
   int tmp_idx = iq_idx;
   float max = 0.0f;
   for(int i=0;i<1024;i++) {
     if( abs( iq_samples[i] ) > max ) max = abs(iq_samples[i]);
   }

   for(int i=0;i<1024;i++) {
     iq_samples[i] = (iq_samples[i]/max)*12000.0f;
   }
 } 

 stroke(0,0,0);
 fill(0,0,0);
 rect(0,256,512,256); //blank to background

 fill(0,255,0);  //foreground colors
 stroke(0,255,0);

  //draw the time-domain IQ
  int idx=0;
  int step=2;
  for(int i=0;i<256;i++) {
    fill(0,255,0);
    stroke(0,255,0);
    float ii1 = iq_samples[idx++]/256.0f;
    float qq1 = iq_samples[idx++]/256.0f;
    if(i>0) line(i*step, 320+ii1, (i*step+1), 320+qq1);

    fill(255,255,255);
    stroke(255,255,255);
    float ii2 = iq_samples[idx++]/256.0f;
    float qq2 = iq_samples[idx++]/256.0f;
    if(i>0) line(i*step, 320+ii2, (i*step+1), 320+qq2);
  }
 
  //draw the XY IQ
  idx=0;
  for(int i=0;i<256;i++) {
    fill(0,255,0);
    stroke(0,255,0);
    float ii1 = iq_samples[idx++]/256.0f;
    float qq1 = iq_samples[idx++]/256.0f;

    float ii2 = iq_samples[idx++]/256.0f;
    float qq2 = iq_samples[idx++]/256.0f;
    if(i>0) line(320+ii1, 192+qq1, 320+ii2, 192+qq2);
  }
}
///////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////
void handle_metainfo(byte[] b, int len) {

  ByteBuffer bb3 = ByteBuffer.wrap(b);
  bb3.order(ByteOrder.LITTLE_ENDIAN);

  bb3.getInt(); //magic
  bb3.getShort(); //port
  bb3.getShort(); //len


  double center_freq_mhz = bb3.getDouble();

  int wacn_id = bb3.getInt();
  int sys_id = (int) (bb3.getShort()&0xffff) ;
  int site_id = (int) (bb3.getShort()&0xffff) ;
  int rfss_id = (int) (bb3.getShort()&0xffff) ;
  int p25_sys_nac = (int) (bb3.getShort()&0xffff) ;

  String loc_str=null;
  String desc_str=null;
  String sysname_str=null;

  byte[] desc = new byte[13];
  for(int i=0;i<13;i++) {
    desc[i] = bb3.get();
    if(desc[i]==0 && desc_str==null) desc_str = new String(desc);
  }

  byte[] loc = new byte[33];
  for(int i=0;i<33;i++) {
    loc[i] = bb3.get();
    if(loc[i]==0 && loc_str==null) loc_str = new String(loc);
  }

  byte[] mac = new byte[6];
  for(int i=0;i<6;i++) {
    mac[i] = bb3.get();
  }

  int lna_gain=bb3.getInt();
  int mixer_gain=bb3.getInt();

  int vga_gain=bb3.getInt();
  vga_gain &= 0x0f;

  float err_hz_f = bb3.getFloat();
  float est_hz_f = bb3.getFloat();
  int synced = (int) (bb3.get()&0xff);

  bb3.get();
  bb3.get();
  bb3.get();

  float nco_offset = bb3.getFloat();
  float loop_freq = bb3.getFloat();
  float evm_db = bb3.getFloat();
  float evm_p = bb3.getFloat();

  int demod = bb3.getInt();
  int did_freq_cc_lock = bb3.getInt();


  int rssi_i = bb3.getInt();	
  int tg_s = bb3.getInt();
  float erate = bb3.getFloat();
  byte status_led = bb3.get();
  byte tg_led = bb3.get();
  byte on_control_b = bb3.get();
  bb3.get();
  int follow = bb3.getInt();

  byte[] sys_name = new byte[32];
  for(int i=0;i<32;i++) {
    sys_name[i] = bb3.get();
    if(sys_name[i]==0 && sysname_str==null) sysname_str = new String(sys_name);
  }

  byte is_control_b = bb3.get();
  byte phase2 = bb3.get();
  bb3.get();
  bb3.get();

  double span_mhz=0.0;
  if(phase2 >0) span_mhz = 0.030;
      else span_mhz = 0.024;

  float rssi_f = bb3.getFloat();

  float squelch = bb3.getFloat();
  int agcmode = bb3.getInt();
  float max_freq_hz = bb3.getFloat();  //two-tone
  byte mode_b = bb3.get();
  byte slot_b = bb3.get();
  bb3.get();
  bb3.get();
  int send_const_count = bb3.getInt();
  byte roam = bb3.get();
  bb3.get();
  bb3.get();
  bb3.get();
  int padding1 = bb3.getInt();
  int global_sync_count = bb3.getInt();
  short tgzone = bb3.getShort();
  bb3.get();
  bb3.get();
  int lsm_std = bb3.getInt();
  byte adc_clip=bb3.get();

  int wio_packet_id= bb3.get();
  if(wio_packet_id<0) { //convert 8-bit signed to 32-bit signed
    wio_packet_id*=-1;
    wio_packet_id= 256-wio_packet_id;
  }

  bb3.get();
  bb3.get();

  int RID = bb3.getInt();
  byte[] alias = new byte[32];
  String alias_str=null;
  for(int i=0;i<32;i++) {
    alias[i] = bb3.get();
    if(alias[i]==0 && alias_str==null) alias_str = new String(alias);
  }

  byte do_wio_lines = bb3.get();

  String wio_line1_str=null;
  byte[] wio_line1 = new byte[32];
  for(int i=0;i<32;i++) {
    wio_line1[i] = bb3.get();
    if(wio_line1[i]==0 && wio_line1_str==null) wio_line1_str = new String(wio_line1);
  }
  
  bb3.get();
  bb3.get();
  
  String wio_line2_str=null;
  byte[] wio_line2 = new byte[64];
  for(int i=0;i<64;i++) {
    wio_line2[i] = bb3.get();
    if(wio_line2[i]==0 && wio_line2_str==null) wio_line2_str = new String(wio_line2);
  }

  bb3.get();
  int wio_timeout = bb3.getInt();

  byte tsbk_sec = bb3.get();
  bb3.get();
  bb3.get();
  bb3.get();
  int total_session_time = bb3.getInt();

  int ref_freq_cal = bb3.getInt();


  String site_name_str=null;
  byte[] site_name = new byte[32];
  for(int i=0;i<32;i++) {
    site_name[i] = bb3.get();
    if(site_name[i]==0 && site_name_str==null) site_name_str = new String(site_name);
  }

  byte data_type = bb3.get();
  bb3.get();
  bb3.get();
  bb3.get();

  byte[] data = new byte[128];
  for(int i=0;i<128;i++) {
    data[i] = bb3.get();
  }

  byte wio_button_mode = bb3.get();
  byte audio_mute = bb3.get();
  byte learn_mode = bb3.get();
  bb3.get();

  short tg_zone = bb3.getShort();
  bb3.get();
  bb3.get();

  int voice_tg_timeout = bb3.getInt();
  int roam_timeout = bb3.getInt();
  short roam_sysid = bb3.getShort();
  bb3.get();
  bb3.get();

  byte inc_in_scan = bb3.get();
  byte loc_nac = bb3.get();
  bb3.get();
  bb3.get();


  col1 = rgb565_to_color( bb3.getShort());
  col2 = rgb565_to_color( bb3.getShort());
  col3 = rgb565_to_color( bb3.getShort());
  col4 = rgb565_to_color( bb3.getShort());
  col5 = rgb565_to_color( bb3.getShort());
  col6 = rgb565_to_color( bb3.getShort());
  col7 = rgb565_to_color( bb3.getShort());
  col8 = rgb565_to_color( bb3.getShort());
  col9 = rgb565_to_color( bb3.getShort());
  col10 = rgb565_to_color( bb3.getShort());
  col11 = rgb565_to_color( bb3.getShort());
  col12 = rgb565_to_color( bb3.getShort());
  col13 = rgb565_to_color( bb3.getShort());
  col14 = rgb565_to_color( bb3.getShort());
  col_menu_fg = rgb565_to_color( bb3.getShort());
  col_menu_bg = rgb565_to_color( bb3.getShort());
  col_def_bg = rgb565_to_color( bb3.getShort());
  col_def_fg = rgb565_to_color( bb3.getShort());
  col_def_led1_on = rgb565_to_color( bb3.getShort());
  col_def_led2_on = rgb565_to_color( bb3.getShort());
  col_def_led1_off = rgb565_to_color( bb3.getShort());
  col_def_led2_off = rgb565_to_color( bb3.getShort());
  col_def_indicator = rgb565_to_color( bb3.getShort());
  col_def_const = rgb565_to_color( bb3.getShort());

  byte padd1 = bb3.get();
  bb3.get();
  bb3.get();
  bb3.get();

  int crc32val = bb3.getInt();




  metamod++;
  if( do_meta_output==1 && ((metamod%128==0 && on_control_b==0) || (metamod%64==0 && on_control_b==1) ) ) {  //only print every 31 packets when on voice channel
    print( String.format("\r\ncol_def_const: %02X", col_def_const) );

    //print("\r\nmeta len "+len);
    print( String.format("\r\n\r\nlna gain: %d", lna_gain) );
    print( String.format("\r\nmix gain: %d", mixer_gain) );
    print( String.format("\r\nvga gain: %d", vga_gain) );
    print( String.format("\r\ntg_s: %d", tg_s) ); 
    print( String.format("\r\nnco_off: %3.3f", nco_offset) ); 
    print( String.format("\r\nnco2_off: %3.3f", loop_freq) ); 
    print( String.format("\r\ndesc: %s", new String(desc_str) ) );
    print( String.format("\r\nloc: %s", new String(loc_str) ) );
    print( String.format("\r\nsysname: %s", new String(sysname_str) ) );
    print( String.format("\r\n%02X:%02X:%02X:%02X:%02X:%02X", mac[0],mac[1],mac[2],mac[3],mac[4],mac[5]) );
    print( String.format("\r\nspan: %3.3f", span_mhz) ); 
    print( String.format("\r\nrssi: %3.0f dBm", rssi_f) ); 

    print( String.format("\r\nwacn: 0x%05X", wacn_id) );
    print( String.format("\r\nsys_id: 0x%03X", sys_id) );
    print( String.format("\r\nnac: 0x%03X", p25_sys_nac) );
    print( String.format("\r\nsite: %d", site_id) );
    print( String.format("\r\nrfss: %d", rfss_id) );
    print( String.format("\r\nfreq_mhz: %3.6f", center_freq_mhz) );
    print( String.format("\r\nwio_packet_id: %d", (int)wio_packet_id) );
    print( String.format("\r\nalias: %s", alias_str) );
    print( String.format("\r\nrid: %d", (int)RID) );
    print( String.format("\r\nsite_name: %s", site_name_str) );
  }
  
 
 if (showText == 1) {
   
    noStroke();
    textSize(30);

      //Line 1
    if (follow >0) { fill(0,0,0); //color
         rect(520, 0, 512, 50); // erase
         fill(col1);
         text("HOLDING ON TG "+follow,550,40);
     }
    else if (wio_line1_str.trim() != "" || wio_line2_str.trim() != "") 
    { textSize(20);
      fill(0,0,0);
      rect(520, 0, 512, 50); // erase
      fill(col1);
      text(wio_line1_str.trim(), 550, 20);
      text(wio_line2_str.trim(), 550, 40);
      fill(0,0,0);
       textSize(30);
      if(wio_line2_str!="") {
        config.setStatus(wio_line2_str);
      }
      else if(wio_line1_str!="") {
        config.setStatus(wio_line2_str);
      }
    }
    else if (wio_line1_str.trim() == "" || wio_line2_str.trim() == "") 
    { fill(0,0,0);
     rect(520, 0, 512, 50); // erase
     fill(col1);
     text(sysname_str.trim()+" / "+site_name_str.trim(),550,40);
   }
   
    //Line2
   fill(0,0,0); //color
    rect(520, 50, 512, 50); // erase
    if (tg_s >0) { fill(col2); // color
       text(tg_s +"  "+desc_str.trim(),550,90);
       TG_active = (loc_str.trim()+"    "+tg_s); }
       
    //Line3
    fill(0,0,0); //color
    rect(520, 100, 512, 50); // erase
    fill(col3);
    text(loc_str.trim(),550,140);

    //Line4
    fill(0,0,0); //color
    rect(520, 150, 512, 50); // erase
    fill(col4);
    textSize(20);
    text(hex(wacn_id,5)+"-"+hex(sys_id,3)+"-"+hex(p25_sys_nac,3)+"  RSSI "+int(rssi_f)+" dBm ",550,190);

    //Line5
    fill(0,0,0); //color
    rect(520, 200, 512, 50); // erase
    fill(col5);
    textSize(20);
    if (demod == 0) dmod = "LSM";
    if (demod == 1) dmod = "FM";
    text("SITE "+site_id+", RFSS "+rfss_id+" DEMOD "+dmod,550,240);

    //Line6
    fill(0,0,0); //color
    rect(520, 250, 512, 50); // erase
    fill(col6);
    textSize(20);
    String eevm = String.format("%2.1f",evm_p);
    String eerate = String.format("%2.4f",erate);
    text("EVM "+eevm+"  ERATE "+eerate,550,290);
      if (on_control_b !=0) text("TSBK/SEC "+tsbk_sec,750,290);
      
    //Line7
    fill(0,0,0); //color
    rect(520, 300, 512, 50); // erase
    fill(col7);
    textSize(25);
    String cc_mhz = String.format("%3.5f", center_freq_mhz);
    text("FREQ: "+cc_mhz+" MHz",550,340); 
    if (phase2 >0)  {fill(col_def_indicator); text("P2",850,340); fill(col7);}
    if (phase2 ==0) {fill(col_def_indicator); text("P1",850,340); fill(col7);}

    //Line8
    fill(0,0,0); //color
    rect(520, 350, 512, 50); // erase
    fill(col8);
    textSize(25);
    if (RID >0 ) {
    text("RID: "+RID+" , "+alias_str.trim(),550,390); };

    //Line9
    if (showTG_history == 1) {
      if (TG_old != TG_active) {
  
         if (tg_s == 0) {    
        
           fill(0,0,0); //color
           rect(520, 400, 512, 50); // erase
           fill(col5); // col5 matches Wio TG history
           textSize(20);
              TG5 = TG4;
              TG4 = TG3;
              TG3 = TG2;
              TG2 = TG1; 
           text(TG_active, 550,420);  
           text(TG2,550,440);
    
    //Line10
           fill(0,0,0); //color
           rect(520, 450, 512, 70); // erase
           fill(col5); // col5 matches Wio TG history
           textSize(20);     
           text(TG3, 550, 460);
           text(TG4, 550, 480);
           text(TG5, 550, 500);
           TG1 = TG_active;
           TG_old = TG_active;  
        }
      }
    } 
  } 
} 
/////////////////////////////////////////////////////
/////////////////////////////////////////////////////
color rgb565_to_color(short val) {
  int r5 = (int)((val>>11)&0x1f);
  int g6 = (int)((val>>5)&0x3f);
  int b5 = (int)(val&0x1f);

  int r8 = (r5 * 527 + 23) >>6;
  int g8 = (g6 * 259 + 23) >>6;
  int b8 = (b5 * 527 + 23) >>6;
  return color(r8,g8,b8);
}
