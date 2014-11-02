#!/bin/bash

# build project
gradle clean build
gradle :scrabble-indexer:installApp
gradle :scrabble-suggester:installApp

# install
if [ ! -d "install" ]; then
	echo "Creating install directory"
	mkdir install
	mv scrabble-indexer/build/install/scrabble-indexer install/
	mv scrabble-suggester/build/install/scrabble-suggester install/
	echo "Completed creating install directory"
fi


if [ ! -d "run" ]; then
	echo "Creating run directory"
	mkdir run
	echo '../install/scrabble-indexer/bin/scrabble-indexer "$@"' >> run/scrabble-indexer
	echo '../install/scrabble-suggester/bin/scrabble-suggester "$@"' >> run/scrabble-suggester
	chmod +x run/scrabble-indexer
	chmod +x run/scrabble-suggester
	echo "Completed creating run directory"
fi