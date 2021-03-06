package io.zeebe.clustertestbench.cloud.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerationInfo {
	
	private String name;
	private String uuid;
	private Map<String, String> versions;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public Map<String, String> getVersions() {
		return versions;
	}
	public void setVersions(Map<String, String> versions) {
		this.versions = versions;
	}
	
	@Override
	public String toString() {
		return "GenerationInfo [name=" + name + ", uuid=" + uuid + ", versions=" + versions + "]";
	}
	
}
