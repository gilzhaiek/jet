package com.reconinstruments.mapImages.objects;

import java.util.HashMap;
import java.util.Map;

import android.util.SparseArray;

public class ResortElementsList {
	public enum EElementArea {
		eAmericas,
		eNotAmericas,
		eBoth
	}
	
	protected class ElementsTypes {
		public Map<String, Integer>	TypesMap = null;
		
		public ElementsTypes(){
			TypesMap = new HashMap<String, Integer>();
		}
	}
	
	protected SparseArray<ElementsTypes> mAmericasShapeTypes	= null;
	protected SparseArray<ElementsTypes> mNotAmericasShapeTypes	= null;
	protected SparseArray<ElementsTypes> mBothShapeTypes		= null;
	
	public ResortElementsList() {
		mAmericasShapeTypes		= new SparseArray<ElementsTypes>();
		mNotAmericasShapeTypes	= new SparseArray<ElementsTypes>();
		mBothShapeTypes			= new SparseArray<ElementsTypes>();
	}
	
	protected SparseArray<ElementsTypes> GetShapeTypes(EElementArea elementArea){
		switch (elementArea){
		case eAmericas : 	return mAmericasShapeTypes;
		case eNotAmericas : return mNotAmericasShapeTypes;
		case eBoth :
		default: 			return mBothShapeTypes;
		}
	}
	
	public void AddElement(EElementArea elementArea, int shpType, int elemTypeValue, String elemTypeStr){
		SparseArray<ElementsTypes> shapeTypes = GetShapeTypes(elementArea);
				
		ElementsTypes tmpElementsList = shapeTypes.get(shpType);
		if(tmpElementsList == null) {
			tmpElementsList = new ElementsTypes();
			shapeTypes.put(shpType, tmpElementsList);
		}
		
		tmpElementsList.TypesMap.put(elemTypeStr, elemTypeValue);
	}
	
	protected boolean HasElementType(SparseArray<ElementsTypes> shapeTypes, int shpType, String elemType){
		ElementsTypes elementsTypes = shapeTypes.get(shpType);
		if(elementsTypes == null) return false;
		
		return (elementsTypes.TypesMap.get(elemType) != null);
	}	
	
	public boolean HasElementType(EElementArea elementArea, int shpType, String elemType) {
		SparseArray<ElementsTypes> shapeTypes = GetShapeTypes(elementArea);
		
		if(HasElementType(shapeTypes, shpType, elemType))
			return true;
		
		shapeTypes = GetShapeTypes(EElementArea.eBoth);
		return HasElementType(shapeTypes, shpType, elemType);
	}
	
	protected Integer GetElementValue(SparseArray<ElementsTypes> shapeTypes, int shpType, String elemType){
		ElementsTypes elementsTypes = shapeTypes.get(shpType);
		if(elementsTypes == null) return null;
		
		return elementsTypes.TypesMap.get(elemType);
	}
	
	public int GetElementValue(EElementArea elementArea, int shpType, String elemType) {
		SparseArray<ElementsTypes> shapeTypes = GetShapeTypes(elementArea);		
		Integer elementValue = GetElementValue(shapeTypes, shpType, elemType);
		if(elementValue != null)
			return elementValue;
		
		shapeTypes = GetShapeTypes(EElementArea.eBoth);
		elementValue = GetElementValue(shapeTypes, shpType, elemType);	
		if(elementValue != null)
			return elementValue;
		
		return -1;
	}
}
