/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.2.12
 * apibuilder 0.14.96 app.apibuilder.io/flow/github/latest/anorm_2_8_parsers
 */
import anorm._

package io.flow.github.v0.anorm.parsers {

  import io.flow.github.v0.anorm.conversions.Standard._

  import io.flow.github.v0.anorm.conversions.Types._

  object ContentsType {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.ContentsType] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "contents_type", prefixOpt: Option[String] = None): RowParser[io.flow.github.v0.models.ContentsType] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.github.v0.models.ContentsType(value)
      }
    }

  }

  object Encoding {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Encoding] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "encoding", prefixOpt: Option[String] = None): RowParser[io.flow.github.v0.models.Encoding] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.github.v0.models.Encoding(value)
      }
    }

  }

  object HookEvent {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.HookEvent] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "hook_event", prefixOpt: Option[String] = None): RowParser[io.flow.github.v0.models.HookEvent] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.github.v0.models.HookEvent(value)
      }
    }

  }

  object NodeType {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.NodeType] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "node_type", prefixOpt: Option[String] = None): RowParser[io.flow.github.v0.models.NodeType] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.github.v0.models.NodeType(value)
      }
    }

  }

  object OwnerType {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.OwnerType] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "owner_type", prefixOpt: Option[String] = None): RowParser[io.flow.github.v0.models.OwnerType] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.github.v0.models.OwnerType(value)
      }
    }

  }

  object Visibility {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Visibility] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "visibility", prefixOpt: Option[String] = None): RowParser[io.flow.github.v0.models.Visibility] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.github.v0.models.Visibility(value)
      }
    }

  }

  object Blob {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Blob] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      content: String = "content",
      encoding: String = "encoding",
      url: String = "url",
      sha: String = "sha",
      size: String = "size",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Blob] = {
      SqlParser.str(prefixOpt.getOrElse("") + content) ~
      io.flow.github.v0.anorm.parsers.Encoding.parser(prefixOpt.getOrElse("") + encoding) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.long(prefixOpt.getOrElse("") + size) map {
        case content ~ encoding ~ url ~ sha ~ size => {
          io.flow.github.v0.models.Blob(
            content = content,
            encoding = encoding,
            url = url,
            sha = sha,
            size = size
          )
        }
      }
    }

  }

  object BlobCreated {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.BlobCreated] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      url: String = "url",
      sha: String = "sha",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.BlobCreated] = {
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.str(prefixOpt.getOrElse("") + sha) map {
        case url ~ sha => {
          io.flow.github.v0.models.BlobCreated(
            url = url,
            sha = sha
          )
        }
      }
    }

  }

  object BlobForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.BlobForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      content: String = "content",
      encoding: String = "encoding",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.BlobForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + content) ~
      io.flow.github.v0.anorm.parsers.Encoding.parser(prefixOpt.getOrElse("") + encoding) map {
        case content ~ encoding => {
          io.flow.github.v0.models.BlobForm(
            content = content,
            encoding = encoding
          )
        }
      }
    }

  }

  object Commit {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Commit] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      sha: String = "sha",
      url: String = "url",
      htmlUrl: String = "html_url",
      authorPrefix: String = "author",
      committerPrefix: String = "committer",
      treePrefix: String = "tree",
      message: String = "message",
      parents: String = "parents",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Commit] = {
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.str(prefixOpt.getOrElse("") + htmlUrl) ~
      io.flow.github.v0.anorm.parsers.Person.parserWithPrefix(prefixOpt.getOrElse("") + authorPrefix) ~
      io.flow.github.v0.anorm.parsers.Person.parserWithPrefix(prefixOpt.getOrElse("") + committerPrefix) ~
      io.flow.github.v0.anorm.parsers.TreeSummary.parserWithPrefix(prefixOpt.getOrElse("") + treePrefix) ~
      SqlParser.str(prefixOpt.getOrElse("") + message) ~
      SqlParser.get[Seq[io.flow.github.v0.models.CommitSummary]](prefixOpt.getOrElse("") + parents) map {
        case sha ~ url ~ htmlUrl ~ author ~ committer ~ tree ~ message ~ parents => {
          io.flow.github.v0.models.Commit(
            sha = sha,
            url = url,
            htmlUrl = htmlUrl,
            author = author,
            committer = committer,
            tree = tree,
            message = message,
            parents = parents
          )
        }
      }
    }

  }

  object CommitForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.CommitForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      message: String = "message",
      tree: String = "tree",
      parents: String = "parents",
      authorPrefix: String = "author",
      committerPrefix: String = "committer",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.CommitForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + message) ~
      SqlParser.str(prefixOpt.getOrElse("") + tree) ~
      SqlParser.get[Seq[String]](prefixOpt.getOrElse("") + parents) ~
      io.flow.github.v0.anorm.parsers.Person.parserWithPrefix(prefixOpt.getOrElse("") + authorPrefix) ~
      io.flow.github.v0.anorm.parsers.Person.parserWithPrefix(prefixOpt.getOrElse("") + committerPrefix) map {
        case message ~ tree ~ parents ~ author ~ committer => {
          io.flow.github.v0.models.CommitForm(
            message = message,
            tree = tree,
            parents = parents,
            author = author,
            committer = committer
          )
        }
      }
    }

  }

  object CommitResponse {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.CommitResponse] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      sha: String = "sha",
      url: String = "url",
      authorPrefix: String = "author",
      committerPrefix: String = "committer",
      message: String = "message",
      treePrefix: String = "tree",
      parents: String = "parents",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.CommitResponse] = {
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      io.flow.github.v0.anorm.parsers.Person.parserWithPrefix(prefixOpt.getOrElse("") + authorPrefix) ~
      io.flow.github.v0.anorm.parsers.Person.parserWithPrefix(prefixOpt.getOrElse("") + committerPrefix) ~
      SqlParser.str(prefixOpt.getOrElse("") + message) ~
      io.flow.github.v0.anorm.parsers.TreeSummary.parserWithPrefix(prefixOpt.getOrElse("") + treePrefix) ~
      SqlParser.get[Seq[io.flow.github.v0.models.TreeSummary]](prefixOpt.getOrElse("") + parents) map {
        case sha ~ url ~ author ~ committer ~ message ~ tree ~ parents => {
          io.flow.github.v0.models.CommitResponse(
            sha = sha,
            url = url,
            author = author,
            committer = committer,
            message = message,
            tree = tree,
            parents = parents
          )
        }
      }
    }

  }

  object CommitSummary {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.CommitSummary] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      sha: String = "sha",
      url: String = "url",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.CommitSummary] = {
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) map {
        case sha ~ url => {
          io.flow.github.v0.models.CommitSummary(
            sha = sha,
            url = url
          )
        }
      }
    }

  }

  object Contents {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Contents] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      `type`: String = "type",
      encoding: String = "encoding",
      size: String = "size",
      name: String = "name",
      path: String = "path",
      content: String = "content",
      sha: String = "sha",
      url: String = "url",
      gitUrl: String = "git_url",
      htmlUrl: String = "html_url",
      downloadUrl: String = "download_url",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Contents] = {
      io.flow.github.v0.anorm.parsers.ContentsType.parser(prefixOpt.getOrElse("") + `type`) ~
      io.flow.github.v0.anorm.parsers.Encoding.parser(prefixOpt.getOrElse("") + encoding) ~
      SqlParser.long(prefixOpt.getOrElse("") + size) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + path) ~
      SqlParser.str(prefixOpt.getOrElse("") + content).? ~
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.str(prefixOpt.getOrElse("") + gitUrl) ~
      SqlParser.str(prefixOpt.getOrElse("") + htmlUrl) ~
      SqlParser.str(prefixOpt.getOrElse("") + downloadUrl) map {
        case typeInstance ~ encoding ~ size ~ name ~ path ~ content ~ sha ~ url ~ gitUrl ~ htmlUrl ~ downloadUrl => {
          io.flow.github.v0.models.Contents(
            `type` = typeInstance,
            encoding = encoding,
            size = size,
            name = name,
            path = path,
            content = content,
            sha = sha,
            url = url,
            gitUrl = gitUrl,
            htmlUrl = htmlUrl,
            downloadUrl = downloadUrl
          )
        }
      }
    }

  }

  object CreateTreeForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.CreateTreeForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      baseTree: String = "base_tree",
      tree: String = "tree",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.CreateTreeForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + baseTree) ~
      SqlParser.get[Seq[io.flow.github.v0.models.TreeForm]](prefixOpt.getOrElse("") + tree) map {
        case baseTree ~ tree => {
          io.flow.github.v0.models.CreateTreeForm(
            baseTree = baseTree,
            tree = tree
          )
        }
      }
    }

  }

  object CreateTreeResponse {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.CreateTreeResponse] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      sha: String = "sha",
      url: String = "url",
      treeResultPrefix: String = "tree_result",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.CreateTreeResponse] = {
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      io.flow.github.v0.anorm.parsers.Tree.parserWithPrefix(prefixOpt.getOrElse("") + treeResultPrefix) map {
        case sha ~ url ~ treeResult => {
          io.flow.github.v0.models.CreateTreeResponse(
            sha = sha,
            url = url,
            treeResult = treeResult
          )
        }
      }
    }

  }

  object Error {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Error] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      resource: String = "resource",
      field: String = "field",
      code: String = "code",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Error] = {
      SqlParser.str(prefixOpt.getOrElse("") + resource) ~
      SqlParser.str(prefixOpt.getOrElse("") + field) ~
      SqlParser.str(prefixOpt.getOrElse("") + code) map {
        case resource ~ field ~ code => {
          io.flow.github.v0.models.Error(
            resource = resource,
            field = field,
            code = code
          )
        }
      }
    }

  }

  object GithubObject {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.GithubObject] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      `type`: String = "type",
      sha: String = "sha",
      url: String = "url",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.GithubObject] = {
      SqlParser.str(prefixOpt.getOrElse("") + `type`) ~
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) map {
        case typeInstance ~ sha ~ url => {
          io.flow.github.v0.models.GithubObject(
            `type` = typeInstance,
            sha = sha,
            url = url
          )
        }
      }
    }

  }

  object Hook {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Hook] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      url: String = "url",
      testUrl: String = "test_url",
      pingUrl: String = "ping_url",
      name: String = "name",
      events: String = "events",
      active: String = "active",
      configPrefix: String = "config",
      updatedAt: String = "updated_at",
      createdAt: String = "created_at",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Hook] = {
      SqlParser.long(prefixOpt.getOrElse("") + id) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.str(prefixOpt.getOrElse("") + testUrl) ~
      SqlParser.str(prefixOpt.getOrElse("") + pingUrl) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.get[Seq[io.flow.github.v0.models.HookEvent]](prefixOpt.getOrElse("") + events) ~
      SqlParser.bool(prefixOpt.getOrElse("") + active) ~
      io.flow.github.v0.anorm.parsers.HookConfig.parserWithPrefix(prefixOpt.getOrElse("") + configPrefix) ~
      SqlParser.get[_root_.org.joda.time.DateTime](prefixOpt.getOrElse("") + updatedAt) ~
      SqlParser.get[_root_.org.joda.time.DateTime](prefixOpt.getOrElse("") + createdAt) map {
        case id ~ url ~ testUrl ~ pingUrl ~ name ~ events ~ active ~ config ~ updatedAt ~ createdAt => {
          io.flow.github.v0.models.Hook(
            id = id,
            url = url,
            testUrl = testUrl,
            pingUrl = pingUrl,
            name = name,
            events = events,
            active = active,
            config = config,
            updatedAt = updatedAt,
            createdAt = createdAt
          )
        }
      }
    }

  }

  object HookConfig {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.HookConfig] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      url: String = "url",
      contentType: String = "content_type",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.HookConfig] = {
      SqlParser.str(prefixOpt.getOrElse("") + url).? ~
      SqlParser.str(prefixOpt.getOrElse("") + contentType).? map {
        case url ~ contentType => {
          io.flow.github.v0.models.HookConfig(
            url = url,
            contentType = contentType
          )
        }
      }
    }

  }

  object HookForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.HookForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      configPrefix: String = "config",
      events: String = "events",
      active: String = "active",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.HookForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      io.flow.github.v0.anorm.parsers.HookConfig.parserWithPrefix(prefixOpt.getOrElse("") + configPrefix) ~
      SqlParser.get[Seq[io.flow.github.v0.models.HookEvent]](prefixOpt.getOrElse("") + events) ~
      SqlParser.bool(prefixOpt.getOrElse("") + active) map {
        case name ~ config ~ events ~ active => {
          io.flow.github.v0.models.HookForm(
            name = name,
            config = config,
            events = events,
            active = active
          )
        }
      }
    }

  }

  object Node {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Node] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      path: String = "path",
      mode: String = "mode",
      `type`: String = "type",
      size: String = "size",
      sha: String = "sha",
      url: String = "url",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Node] = {
      SqlParser.str(prefixOpt.getOrElse("") + path) ~
      SqlParser.str(prefixOpt.getOrElse("") + mode) ~
      io.flow.github.v0.anorm.parsers.NodeType.parser(prefixOpt.getOrElse("") + `type`) ~
      SqlParser.long(prefixOpt.getOrElse("") + size) ~
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) map {
        case path ~ mode ~ typeInstance ~ size ~ sha ~ url => {
          io.flow.github.v0.models.Node(
            path = path,
            mode = mode,
            `type` = typeInstance,
            size = size,
            sha = sha,
            url = url
          )
        }
      }
    }

  }

  object NodeForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.NodeForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      path: String = "path",
      mode: String = "mode",
      `type`: String = "type",
      sha: String = "sha",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.NodeForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + path) ~
      SqlParser.str(prefixOpt.getOrElse("") + mode) ~
      io.flow.github.v0.anorm.parsers.NodeType.parser(prefixOpt.getOrElse("") + `type`) ~
      SqlParser.str(prefixOpt.getOrElse("") + sha) map {
        case path ~ mode ~ typeInstance ~ sha => {
          io.flow.github.v0.models.NodeForm(
            path = path,
            mode = mode,
            `type` = typeInstance,
            sha = sha
          )
        }
      }
    }

  }

  object Person {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Person] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      email: String = "email",
      date: String = "date",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Person] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + email) ~
      SqlParser.get[_root_.org.joda.time.DateTime](prefixOpt.getOrElse("") + date) map {
        case name ~ email ~ date => {
          io.flow.github.v0.models.Person(
            name = name,
            email = email,
            date = date
          )
        }
      }
    }

  }

  object PullRequest {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.PullRequest] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      url: String = "url",
      number: String = "number",
      htmlUrl: String = "html_url",
      headPrefix: String = "head",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.PullRequest] = {
      SqlParser.long(prefixOpt.getOrElse("") + id) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.long(prefixOpt.getOrElse("") + number) ~
      SqlParser.str(prefixOpt.getOrElse("") + htmlUrl) ~
      io.flow.github.v0.anorm.parsers.PullRequestHead.parserWithPrefix(prefixOpt.getOrElse("") + headPrefix) map {
        case id ~ url ~ number ~ htmlUrl ~ head => {
          io.flow.github.v0.models.PullRequest(
            id = id,
            url = url,
            number = number,
            htmlUrl = htmlUrl,
            head = head
          )
        }
      }
    }

  }

  object PullRequestForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.PullRequestForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      title: String = "title",
      head: String = "head",
      base: String = "base",
      body: String = "body",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.PullRequestForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + title) ~
      SqlParser.str(prefixOpt.getOrElse("") + head) ~
      SqlParser.str(prefixOpt.getOrElse("") + base) ~
      SqlParser.str(prefixOpt.getOrElse("") + body).? map {
        case title ~ head ~ base ~ body => {
          io.flow.github.v0.models.PullRequestForm(
            title = title,
            head = head,
            base = base,
            body = body
          )
        }
      }
    }

  }

  object PullRequestHead {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.PullRequestHead] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      ref: String = "ref",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.PullRequestHead] = {
      SqlParser.str(prefixOpt.getOrElse("") + ref) map {
        case ref => {
          io.flow.github.v0.models.PullRequestHead(
            ref = ref
          )
        }
      }
    }

  }

  object Ref {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Ref] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      ref: String = "ref",
      url: String = "url",
      objectPrefix: String = "object",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Ref] = {
      SqlParser.str(prefixOpt.getOrElse("") + ref) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      io.flow.github.v0.anorm.parsers.GithubObject.parserWithPrefix(prefixOpt.getOrElse("") + objectPrefix) map {
        case ref ~ url ~ objectInstance => {
          io.flow.github.v0.models.Ref(
            ref = ref,
            url = url,
            `object` = objectInstance
          )
        }
      }
    }

  }

  object RefForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.RefForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      ref: String = "ref",
      sha: String = "sha",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.RefForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + ref) ~
      SqlParser.str(prefixOpt.getOrElse("") + sha) map {
        case ref ~ sha => {
          io.flow.github.v0.models.RefForm(
            ref = ref,
            sha = sha
          )
        }
      }
    }

  }

  object RefUpdateForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.RefUpdateForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      sha: String = "sha",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.RefUpdateForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + sha) map {
        case sha => {
          io.flow.github.v0.models.RefUpdateForm(
            sha = sha
          )
        }
      }
    }

  }

  object Repository {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Repository] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      ownerPrefix: String = "owner",
      name: String = "name",
      fullName: String = "full_name",
      `private`: String = "private",
      description: String = "description",
      url: String = "url",
      htmlUrl: String = "html_url",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Repository] = {
      SqlParser.long(prefixOpt.getOrElse("") + id) ~
      io.flow.github.v0.anorm.parsers.User.parserWithPrefix(prefixOpt.getOrElse("") + ownerPrefix) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + fullName) ~
      SqlParser.bool(prefixOpt.getOrElse("") + `private`) ~
      SqlParser.str(prefixOpt.getOrElse("") + description).? ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.str(prefixOpt.getOrElse("") + htmlUrl) map {
        case id ~ owner ~ name ~ fullName ~ privateInstance ~ description ~ url ~ htmlUrl => {
          io.flow.github.v0.models.Repository(
            id = id,
            owner = owner,
            name = name,
            fullName = fullName,
            `private` = privateInstance,
            description = description,
            url = url,
            htmlUrl = htmlUrl
          )
        }
      }
    }

  }

  object Tag {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Tag] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      tag: String = "tag",
      sha: String = "sha",
      url: String = "url",
      message: String = "message",
      taggerPrefix: String = "tagger",
      objectPrefix: String = "object",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Tag] = {
      SqlParser.str(prefixOpt.getOrElse("") + tag) ~
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.str(prefixOpt.getOrElse("") + message) ~
      io.flow.github.v0.anorm.parsers.Tagger.parserWithPrefix(prefixOpt.getOrElse("") + taggerPrefix) ~
      io.flow.github.v0.anorm.parsers.GithubObject.parserWithPrefix(prefixOpt.getOrElse("") + objectPrefix) map {
        case tag ~ sha ~ url ~ message ~ tagger ~ objectInstance => {
          io.flow.github.v0.models.Tag(
            tag = tag,
            sha = sha,
            url = url,
            message = message,
            tagger = tagger,
            `object` = objectInstance
          )
        }
      }
    }

  }

  object TagForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.TagForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      tag: String = "tag",
      message: String = "message",
      `object`: String = "object",
      `type`: String = "type",
      taggerPrefix: String = "tagger",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.TagForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + tag) ~
      SqlParser.str(prefixOpt.getOrElse("") + message) ~
      SqlParser.str(prefixOpt.getOrElse("") + `object`) ~
      SqlParser.str(prefixOpt.getOrElse("") + `type`) ~
      io.flow.github.v0.anorm.parsers.Tagger.parserWithPrefix(prefixOpt.getOrElse("") + taggerPrefix) map {
        case tag ~ message ~ objectInstance ~ typeInstance ~ tagger => {
          io.flow.github.v0.models.TagForm(
            tag = tag,
            message = message,
            `object` = objectInstance,
            `type` = typeInstance,
            tagger = tagger
          )
        }
      }
    }

  }

  object TagSummary {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.TagSummary] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      commitPrefix: String = "commit",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.TagSummary] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      io.flow.github.v0.anorm.parsers.CommitSummary.parserWithPrefix(prefixOpt.getOrElse("") + commitPrefix) map {
        case name ~ commit => {
          io.flow.github.v0.models.TagSummary(
            name = name,
            commit = commit
          )
        }
      }
    }

  }

  object Tagger {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Tagger] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      email: String = "email",
      date: String = "date",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Tagger] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + email) ~
      SqlParser.get[_root_.org.joda.time.DateTime](prefixOpt.getOrElse("") + date) map {
        case name ~ email ~ date => {
          io.flow.github.v0.models.Tagger(
            name = name,
            email = email,
            date = date
          )
        }
      }
    }

  }

  object Tree {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.Tree] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      sha: String = "sha",
      url: String = "url",
      truncated: String = "truncated",
      tree: String = "tree",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.Tree] = {
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.bool(prefixOpt.getOrElse("") + truncated) ~
      SqlParser.get[Seq[io.flow.github.v0.models.Node]](prefixOpt.getOrElse("") + tree) map {
        case sha ~ url ~ truncated ~ tree => {
          io.flow.github.v0.models.Tree(
            sha = sha,
            url = url,
            truncated = truncated,
            tree = tree
          )
        }
      }
    }

  }

  object TreeForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.TreeForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      path: String = "path",
      mode: String = "mode",
      `type`: String = "type",
      sha: String = "sha",
      content: String = "content",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.TreeForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + path) ~
      SqlParser.str(prefixOpt.getOrElse("") + mode) ~
      io.flow.github.v0.anorm.parsers.NodeType.parser(prefixOpt.getOrElse("") + `type`) ~
      SqlParser.str(prefixOpt.getOrElse("") + sha).? ~
      SqlParser.str(prefixOpt.getOrElse("") + content).? map {
        case path ~ mode ~ typeInstance ~ sha ~ content => {
          io.flow.github.v0.models.TreeForm(
            path = path,
            mode = mode,
            `type` = typeInstance,
            sha = sha,
            content = content
          )
        }
      }
    }

  }

  object TreeResult {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.TreeResult] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      path: String = "path",
      mode: String = "mode",
      `type`: String = "type",
      size: String = "size",
      sha: String = "sha",
      url: String = "url",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.TreeResult] = {
      SqlParser.str(prefixOpt.getOrElse("") + path) ~
      SqlParser.str(prefixOpt.getOrElse("") + mode) ~
      io.flow.github.v0.anorm.parsers.NodeType.parser(prefixOpt.getOrElse("") + `type`) ~
      SqlParser.long(prefixOpt.getOrElse("") + size) ~
      SqlParser.str(prefixOpt.getOrElse("") + sha) ~
      SqlParser.str(prefixOpt.getOrElse("") + url) map {
        case path ~ mode ~ typeInstance ~ size ~ sha ~ url => {
          io.flow.github.v0.models.TreeResult(
            path = path,
            mode = mode,
            `type` = typeInstance,
            size = size,
            sha = sha,
            url = url
          )
        }
      }
    }

  }

  object TreeSummary {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.TreeSummary] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      url: String = "url",
      sha: String = "sha",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.TreeSummary] = {
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.str(prefixOpt.getOrElse("") + sha) map {
        case url ~ sha => {
          io.flow.github.v0.models.TreeSummary(
            url = url,
            sha = sha
          )
        }
      }
    }

  }

  object UnprocessableEntity {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.UnprocessableEntity] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      message: String = "message",
      errors: String = "errors",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.UnprocessableEntity] = {
      SqlParser.str(prefixOpt.getOrElse("") + message) ~
      SqlParser.get[Seq[io.flow.github.v0.models.Error]](prefixOpt.getOrElse("") + errors).? map {
        case message ~ errors => {
          io.flow.github.v0.models.UnprocessableEntity(
            message = message,
            errors = errors
          )
        }
      }
    }

  }

  object User {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.User] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      login: String = "login",
      name: String = "name",
      email: String = "email",
      avatarUrl: String = "avatar_url",
      gravatarId: String = "gravatar_id",
      url: String = "url",
      htmlUrl: String = "html_url",
      `type`: String = "type",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.User] = {
      SqlParser.long(prefixOpt.getOrElse("") + id) ~
      SqlParser.str(prefixOpt.getOrElse("") + login) ~
      SqlParser.str(prefixOpt.getOrElse("") + name).? ~
      SqlParser.str(prefixOpt.getOrElse("") + email).? ~
      SqlParser.str(prefixOpt.getOrElse("") + avatarUrl).? ~
      SqlParser.str(prefixOpt.getOrElse("") + gravatarId).? ~
      SqlParser.str(prefixOpt.getOrElse("") + url) ~
      SqlParser.str(prefixOpt.getOrElse("") + htmlUrl) ~
      io.flow.github.v0.anorm.parsers.OwnerType.parser(prefixOpt.getOrElse("") + `type`) map {
        case id ~ login ~ name ~ email ~ avatarUrl ~ gravatarId ~ url ~ htmlUrl ~ typeInstance => {
          io.flow.github.v0.models.User(
            id = id,
            login = login,
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            gravatarId = gravatarId,
            url = url,
            htmlUrl = htmlUrl,
            `type` = typeInstance
          )
        }
      }
    }

  }

  object UserEmail {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.github.v0.models.UserEmail] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      email: String = "email",
      verified: String = "verified",
      primary: String = "primary",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.github.v0.models.UserEmail] = {
      SqlParser.str(prefixOpt.getOrElse("") + email) ~
      SqlParser.bool(prefixOpt.getOrElse("") + verified) ~
      SqlParser.bool(prefixOpt.getOrElse("") + primary) map {
        case email ~ verified ~ primary => {
          io.flow.github.v0.models.UserEmail(
            email = email,
            verified = verified,
            primary = primary
          )
        }
      }
    }

  }

}