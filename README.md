# bdrc-auth-lib 
Bdrc authorization library based on auth0

### Releasing on Maven

```
mvn -DperformRelease=true clean deploy
```

### Configuration

The lib reads the system property `io.bdrc.auth.propfile.path` when initialize and reads the necessary properties from the file in this path.
