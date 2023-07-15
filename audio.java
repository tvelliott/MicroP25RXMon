//MIT License
//
//Copyright (c) 2022 tvelliott

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

import java.nio.*;
import javax.sound.sampled.*;


class audio {

  java.util.Timer utimer;
  volatile int do_new_audio=0;
  byte[] outbytes=null;
  byte[] inbytes=null;
  int audio_len;
  
  private int demod=0;

  final int BUFFER_LEN = 12000; //good values are between 8000 and 12000
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  class audio_thread extends java.util.TimerTask {

      public void run()
      {
        try {
          while(true) {
            if(do_new_audio>0) {

              ByteBuffer bb = ByteBuffer.wrap(inbytes);
              bb.order(ByteOrder.LITTLE_ENDIAN);

              int idx=0;
              for(int i=0;i<audio_len/2;i++) {

                short val = bb.getShort(); 
              
                outbytes[idx+0] = (byte) (val&0xff); 
                outbytes[idx+1] = (byte) (val>>8);

                outbytes[idx+2] = outbytes[idx+0]; 
                outbytes[idx+3] = outbytes[idx+1]; 

                idx+=4;
              }

             
              sourceDataLine.write(outbytes,0,idx); 

              if(audio_tick_cnt<30) {
                sourceDataLine.start(); 
                audio_tick_cnt=30;
              }
              do_new_audio=0;
              if(audio_active<30) audio_active++;
            }
            else if( sourceDataLine.available()>BUFFER_LEN/2) {
              int idx=0;
              for(int i=0;i<320;i++) {

                outbytes[idx+0] = 0; 
                outbytes[idx+1] = 0; 

                outbytes[idx+2] = outbytes[idx+0]; 
                outbytes[idx+3] = outbytes[idx+1]; 

                idx+=4;
              }
              sourceDataLine.write(outbytes,0,idx); 

              sourceDataLine.start(); 
              audio_tick_cnt=30;
              if(audio_active>0) audio_active--;

            }

            
            Thread.sleep(0, 100);
            //System.out.println("avail: "+sourceDataLine.available());
          }

        } catch(Exception e) {
          e.printStackTrace();
        }
      }
  }
  
public int audio_active=0;
public int audio_tick_cnt=0;
int audio_buf_cnt=0;
AudioFormat format;
SourceDataLine sourceDataLine;

  ///////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////
  public audio() {
    format = new AudioFormat(8000, 16, 2, true, false); //last boolean is endian-type (false=little)
    try {
      sourceDataLine = AudioSystem.getSourceDataLine( format);
      sourceDataLine.open(format, BUFFER_LEN);
    } catch(Exception e) {
      e.printStackTrace();
    }
    
    try {
      utimer = new java.util.Timer(); 
      utimer.schedule( new audio_thread(), 100, 1);
    } catch(Exception e) {
      e.printStackTrace();
    }
    
    if(inbytes==null ) inbytes = new byte[320];
    if(outbytes==null ) outbytes = new byte[ 320*4 ]; 
  }
    
  ///////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////
  public void audio_tick() {
    if(audio_tick_cnt>0) {
      audio_tick_cnt--;
      if(audio_tick_cnt==0) {
        audio_buf_cnt=0;
      }
    }
    if(audio_buf_cnt==0 && sourceDataLine.available()==BUFFER_LEN) sourceDataLine.stop();
  }
  ///////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////
  void play_audio(byte[] b, int len, int demod_mode) {

    while(do_new_audio==1);
    
    demod = demod_mode;

    if(inbytes==null || inbytes.length!=len) inbytes = new byte[len];
    if(outbytes==null || outbytes.length!=len*4) outbytes = new byte[ len*4 ]; 

    
    for(int i=0;i<len;i++) {
      inbytes[i] = b[i];
      
    }
    
      audio_len = len;
      do_new_audio=1;

  }
}
