import java.nio.*;
import javax.sound.sampled.*;


class audio {

  java.util.Timer utimer;
  volatile int do_new_audio=0;
  byte[] outbytes;
  byte[] inbytes;
  int audio_len;

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
              int idx2=0;
              for(int i=0;i<audio_len/2;i++) {

                short val = bb.getShort(); 
              
                outbytes[idx+0] = (byte) (val&0xff); 
                outbytes[idx+1] = (byte) (val>>8);

                outbytes[idx+2] = outbytes[idx+0]; 
                outbytes[idx+3] = outbytes[idx+1]; 

                idx+=4;
                idx2+=2;
              }

             
              sourceDataLine.write(outbytes,0,idx); 
              //if(frame_cnt++%9==0 && !sourceDataLine.isRunning()) sourceDataLine.start();
              if(audio_buf_cnt++>4) {
                audio_tick_cnt=30;
                sourceDataLine.start();
              }
            }

            do_new_audio=0;
            Thread.sleep(0, 100);
          }

        } catch(Exception e) {
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
    format = new AudioFormat(7950, 16, 2, true, false); //last boolean is endian-type (false=little)
    try {
      sourceDataLine = AudioSystem.getSourceDataLine( format);
      sourceDataLine.open(format, 8000);
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
      }
    }
  }
  ///////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////
  void play_audio(byte[] b, int len) {

    while(do_new_audio==1);

    if(inbytes==null || inbytes.length!=len) inbytes = new byte[len];
    if(outbytes==null || outbytes.length!=len*4) outbytes = new byte[ len*4 ]; 

    for(int i=0;i<len;i++) {
      inbytes[i] = b[i];
    }

    audio_len = len;
    do_new_audio=1;
  }
}
