package info.codesaway.castlesearching;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.Consumer;

public class WatchDirectory implements Runnable {
	// https://docs.oracle.com/javase/tutorial/essential/io/notification.html
	private static final WatchService WATCHER = createWatcher();

	public Path directory;
	public Consumer<Path> consumer;

	public WatchDirectory(final Path directory, final Consumer<Path> consumer) {
		this.directory = directory;
		this.consumer = consumer;

		// System.out.println("Register directory: " + directory);
		register(directory);
	}

	private static WatchService createWatcher() {
		try {
			return FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			// Should never occur per Javadocs
			// (if it does, then don't enable watching of setting changes)
			e.printStackTrace();
			return null;
		}
	}

	public static WatchKey register(final Path directory) {
		if (WATCHER == null) {
			return null;
		}

		try {
			return directory.register(WATCHER, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		} catch (IOException x) {
			return null;
		}
	}

	@Override
	public void run() {
		if (WATCHER == null) {
			return;
		}

		// Infinite loop
		// (should be run in it's own thread)
		while (true) {
			// wait for key to be signaled
			WatchKey key;
			try {
				// System.out.println("Waiting for file change");
				key = WATCHER.take();
			} catch (InterruptedException x) {
				// https://stackoverflow.com/a/42727813/12610042
				Thread.currentThread().interrupt();
				return;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();

				// This key is registered only
				// for ENTRY_CREATE events,
				// but an OVERFLOW event can
				// occur regardless if events
				// are lost or discarded.
				if (kind == OVERFLOW) {
					continue;
				}

				// The filename is the
				// context of the event.
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path filename = ev.context();

				// Verify that the new
				// file is a text file.
				// try {
				// Resolve the filename against the directory.
				// If the filename is "test" and the directory is "foo",
				// the resolved name is "test/foo".
				Path path = this.directory.resolve(filename);

				// System.out.println("Handle change in " + path);

				this.consumer.accept(path);
				// if (!Files.probeContentType(child).equals("text/plain"))
				// {
				// // System.err.format("New file '%s'" + " is not a plain
				// // text file.%n", filename);
				// continue;
				// }
				// } catch (IOException x) {
				// // System.err.println(x);
				// continue;
				// }
			}

			// Reset the key -- this step is critical if you want to
			// receive further watch events. If the key is no longer valid,
			// the directory is inaccessible so exit the loop.
			boolean valid = key.reset();
			if (!valid) {
				break;
			}
		}
	}
}
