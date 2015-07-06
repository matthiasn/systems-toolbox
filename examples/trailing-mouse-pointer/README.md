# systems-toolbox - Trailing Mouse Example

## Usage

Before the first usage, you need to compile the ClojureScript into a JavaScript file:

    lein cljsbuild auto
    
You'll also want the **[Bower](http://bower.io)** dependencies:

    bower install

Once these steps are done, you can start the application as usual:

    lein run

To automatically update the application as you make changes, open another terminal:

    lein figwheel

Now, you can open **[http://localhost:8010/](http://localhost:8010/)** and start interacting with the application:

## License

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
