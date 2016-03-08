# systems-toolbox - Trailing Mouse Example

## Usage

Before the first usage, you want to install the **[Bower](http://bower.io)** dependencies:

    $ bower install

Once this is done, you can start the application as usual:

    $ lein run

This will run the application on **[http://localhost:8888/](http://localhost:8888/)**. However, we will still need to compile the ClojureScript:

    $ lein cljsbuild auto release

This will compile the ClojureScript into JavaScript using `:advanced` optimization.

You can also use **[Figwheel](https://github.com/bhauman/lein-figwheel)** to automatically update the application as you make changes. For that, open another terminal:

    $ lein figwheel

You can then for example inspect the state of a component by using the following commands in the Figwheel REPL:

    (require '[matthiasn.systems-toolbox.switchboard :as sb])
    (require '[example.core :as c])
    (sb/send-cmd c/switchboard [:cmd/print-cmp-state :client/histogram-cmp])

By default, the webserver exposed by the systems-toolbox library listens on port 8888 and only binds to the localhost interface. You can use environment variables to change this behavior, for example:

    $ HOST="0.0.0.0" PORT=8010 lein run

## License

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
