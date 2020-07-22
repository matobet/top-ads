# top-ads

## Ads analytics and processing tool

## Building and running

Dependencies: sbt

```
$ sbt "run impressions.json clicks.json"
```

alternatively

```
$ sbt assembly
$ java -jar target/scala-2.13/top-ads-assembly-0.1.0-SNAPSHOT.jar impressions.json clicks.json
```

The tool takes as command line arguments 2 input JSON files: `impressions.json` and `clicks.json` containing
the JSON arrays of impressions and clicks respectively. Malformed impression or click objects are ignored.

After run the `metrics.json` and `recommendations.json` output files are produced, containing the App/Country
aggregate metrics and per App/Country advertiser recommendations respectively.
