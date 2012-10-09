SkyscrapAR-extractor
====================

Source code extractor for [SkyscrapAR]. Extracts information from git repositories.

Run

    ./compile.sh

Then, get some git repositories, for example, by running

    ./get-repos.sh

Rename one of the repositories to git-repos, then run

    ./extract.sh

It will generate the file SCMtoXML2.xml. Use it as input to [SkyscrapAR].


[SkyscrapAR]: https://github.com/rodrigorgs/SkyscrapAR

Dependencies
============

For your convenience, the `lib` folder with the dependencies is available at http://app.dcc.ufba.br/~rodrigo/SkyscrapAR-extractor-lib.zip