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
  volatile byte[] inbytes;
  volatile byte[] outbytes;
  volatile short out_s=0;
  volatile short out_e=0;
  volatile int audio_cnt;

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  class audio_thread extends java.util.TimerTask {

      public void run()
      {
        try {
         while(true) {

           if(out_s!=out_e) {

              int idx=0;
              while(out_s!=out_e) { 

                outbytes[idx+0] = (byte) inbytes[out_s++]; 
                if(out_s<0) out_s=0;
                outbytes[idx+1] = (byte) inbytes[out_s++]; 
                if(out_s<0) out_s=0;

                outbytes[idx+2] = outbytes[idx+0]; 
                outbytes[idx+3] = outbytes[idx+1]; 

                idx+=4;
              }

              sourceDataLine.write(outbytes,0,idx); 

              audio_tick_cnt=60;
              if(audio_cnt++>3) sourceDataLine.start();
            }

            if(audio_tick_cnt==10) {
              for(int i=0;i<160*4;i++) {
                outbytes[i]=0;
              }
              sourceDataLine.write(outbytes,0,160*2); 
              sourceDataLine.start();
            }

            Thread.sleep(0, 100);
          }

        } catch(Exception e) {
          e.printStackTrace();
        }
      }
  }
  
public int audio_tick_cnt=0;
int audio_buf_cnt=0;
AudioFormat format;
SourceDataLine sourceDataLine;

  ///////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////
  public audio() {
    format = new AudioFormat(7900, 16, 2, true, false); //last boolean is endian-type (false=little)
    inbytes = new byte[32768];
    outbytes = new byte[32768];
    try {
      sourceDataLine = AudioSystem.getSourceDataLine( format);
      sourceDataLine.open(format, 640*8);
    } catch(Exception e) {
      e.printStackTrace();
    }
    
    try {
      utimer = new java.util.Timer(); 
      utimer.schedule( new audio_thread(), 100, 1);
    } catch(Exception e) {
      e.printStackTrace();
    }
    
  }
    
  ///////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////
  public void audio_tick() {
    if(audio_tick_cnt>0) {
      audio_tick_cnt--;
      if(audio_tick_cnt==0) {
        sourceDataLine.stop();
        audio_buf_cnt=0;
        audio_cnt=0;
      }
    }
  }
  ///////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////
  void play_audio(byte[] b, int len) {
    for(int i=0;i<len;i++) {
      inbytes[out_e++] = b[i];
      if(out_e<0) out_e=0;
    }
  }
}
