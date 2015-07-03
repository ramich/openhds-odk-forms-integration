package org.openhds.test;

import java.util.Arrays;

import org.openhds.test.service.CreateDatabaseDummy;
import org.openhds.test.service.DatabaseStructure;
import org.openhds.test.service.DirectoryWatcher;
import org.openhds.test.service.MirthDummy;
import org.openhds.test.service.CreateTableEnhanced;
import org.openhds.test.service.MirthProperties;
import org.openhds.test.service.WebserviceDummy;
import org.openhds.test.service.WebsiteDummy;
import org.openhds.test.unused.DataTransfer;
import org.openhds.test.unused.PrintTable;

public class Main {

	public Main(){
//		new DatabaseStructure();
//		new DataTransfer();
		
		DirectoryWatcher directoryWatcher = new DirectoryWatcher();
		
		WebserviceDummy webServiceDummy = new WebserviceDummy();
		MirthDummy mirthDummy = new MirthDummy();
		
		directoryWatcher.addListener(mirthDummy);
		directoryWatcher.addListener(webServiceDummy);
		
		String[] dir = MirthProperties.getDirectoriesToWatch();
		System.out.println(Arrays.toString(dir));
		
//		new WebsiteDummy();
//		new CreateDatabaseDummy();

//		new PrintTable();
//		new CreateTableEnhanced();
	}
		


	public static void main(String[] args) {
		new Main();
	}

}
