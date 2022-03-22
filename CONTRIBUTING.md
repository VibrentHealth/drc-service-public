# Development Guide

## Java Standards

### Coding Standards
Adhere to various coding standards

### Logging Standards
Logging is critical to understanding how well our applications are running right now and for determining the causes of situations that happened in the past.

The data gathered thru logs provides extremely valuable information for monitoring of the system and alerting when it is not operating as we expect it to.

Having a good system to gather logs, storing the logs in a way that makes it easy to extract and correlate information and ensuring that we are logging the most relevant information ensures that we are getting the most out of our logging (and alerting and monitoring and tracing).

SLF4J is a facade in front of an actual logging framework, which is logback in this project. SLF4J provides 5 logging levels as listed as followings.

* Logging runtime information from within code via `LoggerFactory`

  |Error Level|Details|
  |---|---|
  |`log.error()`|This log level should be used when the code flow has reached to a failed state and needs Ops attention;eg. data corruption, exceptions, external dependency errors|
  |`log.warn()`|This log level should be used when something abnormal has happened but does not cause any breakage|
  |`log.info()`|This log level should be used for all informative scenarios where the information highlights normal operating conditions|
  |`log.debug()`|This log level should be used for all debugging and investigative scenarios where deep code level behaviour in production environments can be toggled on for debugging and investigation|

* Logging configuration to `logback.xml`
  The `logback.xml` file allows granular control over what log output is visible and what stays hidden.

  |Log Level|Situations to toggle the Level|
  |---|---|
  |ERROR| Indicates that the system is in an unusable state or will be if not looked at; data corruption|
  |WARN| Indicates that something abnormal has happened|
  |INFO| Indicates that something reasonably normal has happened|
  |DEBUG| Additional information that can help to debug issues|
  |TRACE| Additional information about the state of the system while going through the code|

* For more details look [here](https://agile.vignetcorp.com:8086/confluence/display/AC/Logging)


## Code commits
A message should accompany all code commits, no commits should be made without a message. Follow the guidelines for code commits as detailed further

### Commit message labeling and formatting <a name="commitMessages"></a>

We will be using a [changelog generator](https://github.com/conventional-changelog/conventional-changelog) so we would like to format our commit messages in certain way so that our changelog generator picks the messages up properly.

Commit Messages should be formatted as such:
```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```
* `<type>` is one of the types of commit/PR
* `<scope>` is the feature scope _it is typically one word_
* `<subject>` is one-liner description of the feature, mostly derived from the Jira ticket 

#### Allowed types of Commit/PR <a name="commitTypes"></a>
The value for `<type>` Must be one of the following:

|Types|Description|
|---|---|
|`feat`|A new feature being developed|
|`fix`|A bugfix being developed|
|`docs`|For all and any documentation changes|
|`style`|For all styling and formatting changes|
|`refactor`|No change in functionality but only changing implementation design/approach|
|`perf`|A code change that improves performance|
|`test`|A code change adds missing or corrects existing tests|
|`chore`|Any code change to the build process or auxiliary tools and libraries such as documentation generation|

##### Example scenarios of commit messages
* Feature commit  
A basic commit message for a feature should look like this:
  ```
  feat(login): describe the feature here

  specific commit message

  Ref JIRA AC-TICKETNUMBERHERE
  ```

* Bugfix commit  
A basic commit message for a bug should look like this:

  ```
  fix(form-rendering): describe the bug fix here

  specific commit message

  Fixes JIRA AC-TICKETNUMBERHERE
  ```

For more details look [here](https://github.com/conventional-changelog/conventional-commits-parser).

We will use this [changelog](CHANGELOG.MD) so that everyone from QA to management to development will know what is in what build without any confusion.
