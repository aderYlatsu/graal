/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.nodes;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;

import com.oracle.graal.graph.Node;
import com.oracle.graal.graph.NodeClass;
import com.oracle.graal.graph.spi.Canonicalizable;
import com.oracle.graal.graph.spi.CanonicalizerTool;
import com.oracle.graal.nodeinfo.NodeInfo;
import com.oracle.graal.nodes.spi.LIRLowerable;
import com.oracle.graal.nodes.spi.Lowerable;
import com.oracle.graal.nodes.spi.LoweringTool;
import com.oracle.graal.nodes.spi.NodeLIRBuilderTool;

@NodeInfo
public final class DynamicDeoptimizeNode extends AbstractDeoptimizeNode implements LIRLowerable, Lowerable, Canonicalizable {
    public static final NodeClass<DynamicDeoptimizeNode> TYPE = NodeClass.create(DynamicDeoptimizeNode.class);
    @Input ValueNode actionAndReason;
    @Input ValueNode speculation;

    public DynamicDeoptimizeNode(ValueNode actionAndReason, ValueNode speculation) {
        super(TYPE, null);
        this.actionAndReason = actionAndReason;
        this.speculation = speculation;
    }

    public ValueNode getActionAndReason() {
        return actionAndReason;
    }

    public ValueNode getSpeculation() {
        return speculation;
    }

    @Override
    public ValueNode getActionAndReason(MetaAccessProvider metaAccess) {
        return getActionAndReason();
    }

    @Override
    public ValueNode getSpeculation(MetaAccessProvider metaAccess) {
        return getSpeculation();
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        generator.getLIRGeneratorTool().emitDeoptimize(generator.operand(actionAndReason), generator.operand(speculation), generator.state(this));
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (actionAndReason.isConstant() && speculation.isConstant()) {
            JavaConstant constant = actionAndReason.asJavaConstant();
            JavaConstant speculationConstant = speculation.asJavaConstant();
            DeoptimizeNode newDeopt = new DeoptimizeNode(tool.getMetaAccess().decodeDeoptAction(constant), tool.getMetaAccess().decodeDeoptReason(constant), tool.getMetaAccess().decodeDebugId(
                            constant), speculationConstant, stateBefore());
            return newDeopt;
        }
        return this;
    }
}
