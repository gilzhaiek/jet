package com.reconinstruments.mapImages;

import com.reconinstruments.mapImages.mapdata.DataSourceManager;

interface IMapDataService {
	Bundle	getResortID();
	Bundle  getResortGraphicObjects();
	Bundle  getListOfBuddies();
	void	updateLocation(out Bundle bundle);
	void    debugForceLocation(in Location location);
	void    registerStateListener(in DataSourceManager dsm);
	boolean hasData();
	void startGenerateImage();
	void stopGenerateImage();
}