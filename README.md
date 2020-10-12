# bdrc-auth-lib 
Bdrc authorization library based on auth0

### Releasing on Maven

```
mvn -DperformRelease=true clean deploy
```

See [https://buda-base.github.io/bdrc-auth-lib/](https://buda-base.github.io/bdrc-auth-lib/)

Setting up and using mkdocs ([https://www.mkdocs.org/](https://www.mkdocs.org/))

**Install:**

MacOS

[https://www.jeannot-muller.com/how-to-install-mkdocs-on-macos/](https://www.jeannot-muller.com/how-to-install-mkdocs-on-macos/)

**Linux**

[http://learn.openwaterfoundation.org/owf-learn-mkdocs/install/#install-on-linux](http://learn.openwaterfoundation.org/owf-learn-mkdocs/install/#install-on-linux)

**To edit buda-edit documentation website:**

1. clone bdrc-auth-lib repo

2. cd documentation/docs and edit markdown files.

**To build the site:**

In bdrc-auth-lib/documentation, run `sudo mkdocs build`

**To serve it locally:** run `mkdocs serve` (it will be served on localhost:8000)

**To deploy it :** run `mkdocs gh-deploy` (it will be served on [https://buda-base.github.io/bdrc-auth-lib/](https://buda-base.github.io/bdrc-auth-lib/))