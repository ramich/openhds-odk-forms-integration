package org.openhds.test.service.interfaces;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public interface DirectoryWatcherListener {

	public void onDirectoryChanged(WatchEvent.Kind<?> kind, Path fileName, Path dir);
}
