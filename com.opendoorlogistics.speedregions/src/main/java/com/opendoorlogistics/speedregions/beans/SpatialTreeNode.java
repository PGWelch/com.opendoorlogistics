/*
 * Copyright 2016 Open Door Logistics Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opendoorlogistics.speedregions.beans;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opendoorlogistics.speedregions.processor.RegionProcessorUtils;

/**
 * Spatial tree node with support for JSON serialisation / deserialisation
 * @author Phil
 *
 */
public class SpatialTreeNode extends JSONToString{
	private Bounds bounds;
	private String regionId;
	private long assignedPriority;
	private List<SpatialTreeNode> children = new ArrayList<SpatialTreeNode>(0);

	public SpatialTreeNode() {

	}

	public SpatialTreeNode(SpatialTreeNode deepCopyThis) {
		copyNonChildFields(deepCopyThis, this);

		for (SpatialTreeNode childToCopy : deepCopyThis.getChildren()) {
			this.children.add(new SpatialTreeNode(childToCopy));
		}

	}

	public static void copyNonChildFields(SpatialTreeNode deepCopyThis, SpatialTreeNode copyToThis) {
		if (deepCopyThis.getBounds() != null) {
			copyToThis.setBounds(new Bounds(deepCopyThis.getBounds()));
		} else {
			copyToThis.setBounds(null);

		}
		copyToThis.setRegionId(deepCopyThis.getRegionId());
		copyToThis.setAssignedPriority(deepCopyThis.getAssignedPriority());
	}

	public Bounds getBounds() {
		return bounds;
	}

	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
	}

	/**
	 * In the built quadtree children are sorted by highest priority (numerically lowest) first
	 * @return
	 */
	public List<SpatialTreeNode> getChildren() {
		return children;
	}

	public void setChildren(List<SpatialTreeNode> children) {
		this.children = children;
	}

	/**
	 * Only leaf nodes have assigned region ids
	 * @return
	 */
	public String getRegionId() {
		return regionId;
	}

	public void setRegionId(String assignedRegionId) {
		this.regionId = assignedRegionId;
	}

	/**
	 * For a leaf node the priority is the priority of its assigned region.
	 * For non-leaf nodes the priority is the highest (numerically lowest)
	 * priority of its children
	 * @return
	 */
	public long getAssignedPriority() {
		return assignedPriority;
	}

	public void setAssignedPriority(long assignedPriority) {
		this.assignedPriority = assignedPriority;
	}


	@JsonIgnore
	public String toJSON() {
		return RegionProcessorUtils.toJSON(this);
	}

	@Override
	public String toString() {
		// ensure we're not printing subclass fields like geometry which aren't json-friendly
		return new SpatialTreeNode(this).toJSON();
	}

	

	public static SpatialTreeNode fromJSON(String json) {
		return RegionProcessorUtils.fromJSON(json, SpatialTreeNode.class);
	}
}
