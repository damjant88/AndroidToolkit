package androidtoolkit.backend.dto;

public record ProjectRequest(String name, String remoteApkLocation, String localApkFolder, String localLogFolder, String figmaLink, String figmaLinkIos, String confluenceParentPageId, String confluenceArtifactsPageId) {}
