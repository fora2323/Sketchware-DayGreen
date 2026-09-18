package mod.pranav.dependency.resolver

object MavenRepositories {
    val DEFAULT_REPOS = """
      |[
      |    {"url": "https://repo.maven.apache.org/maven2", "name": "Apache Maven Central"},
      |    {"url": "https://dl.google.com/dl/android/maven2", "name": "Google Maven"},
      |    {"url": "https://oss.sonatype.org/content/repositories/releases", "name": "Sonatype Releases"},
      |    {"url": "https://oss.sonatype.org/content/repositories/snapshots", "name": "Sonatype Snapshots"},
      |    {"url": "https://s01.oss.sonatype.org/content/repositories/releases", "name": "Sonatype S01 Releases"},
      |    {"url": "https://jitpack.io", "name": "JitPack"},
      |    {"url": "https://repo.spring.io/plugins-release", "name": "Spring Plugins"},
      |    {"url": "https://repo.spring.io/libs-milestone", "name": "Spring Milestone"},
      |    {"url": "https://maven.atlassian.com/content/repositories/atlassian-public", "name": "Atlassian"},
      |    {"url": "https://repo.hortonworks.com/content/repositories/releases", "name": "Hortonworks"}
      |]
    """.trimMargin()
}