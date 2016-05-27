package com.reconinstruments.dashlauncher.radar.maps;

interface IMapsService {
	Bundle	getResortID();
	Bundle  getResortGraphicObjects();
	Bundle  getListOfBuddies();
	void	updateLocation(out Bundle bundle);
}