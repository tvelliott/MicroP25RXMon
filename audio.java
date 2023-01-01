import java.nio.*;
import javax.sound.sampled.*;


class audio {
  
int audio_tick_cnt=0;
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

    byte[] outbytes = new byte[ len*4 ]; 

    ByteBuffer bb = ByteBuffer.wrap(b);
    bb.order(ByteOrder.LITTLE_ENDIAN);

    int idx=0;
    int idx2=0;
    for(int i=0;i<len/2;i++) {

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
}
