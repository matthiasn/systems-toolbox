# systems-toolbox - Trailing Mouse Example

## Usage

Before the first usage, you want to install the **[Bower](http://bower.io)** dependencies:

    bower install

Once this is done, you can start the application as usual:

    lein run

To automatically update the application as you make changes, open another terminal:

    lein figwheel

Now, you can open **[http://localhost:8010/](http://localhost:8010/)** and start interacting with the application.

You can then for example inspect the state of a component by using the following commands in the Figwheel REPL:

    (require '[matthiasn.systems-toolbox.switchboard :as sb])
    (require '[example.core :as c])
    (sb/send-cmd c/switchboard [:cmd/print-cmp-state :client/histogram-cmp])

Alternatively, for production, you use

    lein cljsbuild auto release

This will compile the ClojureScript into JavaScript using `:advanced` optimization.

## License

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
