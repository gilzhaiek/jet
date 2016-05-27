package com.reconinstruments.heading;
import com.reconinstruments.heading.ICallback;

interface IHeadingService {
	  void register(ICallback cb);
  	  void unregister(ICallback cb);			
}
