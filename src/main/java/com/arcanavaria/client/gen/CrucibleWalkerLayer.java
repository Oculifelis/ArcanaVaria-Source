package com.arcanavaria.client.gen;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Written by tools/bossmodels/crucible_walker.py from the same spec that paints the texture. Edit the spec, not this file. */
public final class CrucibleWalkerLayer {
    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(338, 45).addBox(-11F, 4F, -12F, 22F, 6F, 24F, new CubeDeformation(0F))
            .texOffs(290, 0).addBox(-15F, -4F, -15F, 30F, 8F, 30F, new CubeDeformation(0F))
            .texOffs(0, 0).addBox(-17F, -14F, -17F, 34F, 10F, 34F, new CubeDeformation(0F))
            .texOffs(137, 0).addBox(-19F, -18F, -19F, 38F, 4F, 38F, new CubeDeformation(0F))
            .texOffs(0, 169).addBox(-19F, -22F, -19F, 38F, 4F, 3F, new CubeDeformation(0F))
            .texOffs(83, 169).addBox(-19F, -22F, 16F, 38F, 4F, 3F, new CubeDeformation(0F))
            .texOffs(0, 45).addBox(-19F, -22F, -16F, 3F, 4F, 32F, new CubeDeformation(0F))
            .texOffs(71, 45).addBox(16F, -22F, -16F, 3F, 4F, 32F, new CubeDeformation(0F))
            .texOffs(142, 45).addBox(-16F, -21F, -16F, 32F, 3F, 32F, new CubeDeformation(0F))
            .texOffs(9, 158).addBox(-4F, -19F, -24F, 8F, 4F, 5F, new CubeDeformation(0F))
            .texOffs(53, 177).addBox(-3F, -16F, -27F, 6F, 3F, 3F, new CubeDeformation(0F))
            .texOffs(280, 82).addBox(-18.5F, -15F, -18.5F, 2F, 19F, 2F, new CubeDeformation(0F))
            .texOffs(289, 82).addBox(-18.5F, -15F, 16.5F, 2F, 19F, 2F, new CubeDeformation(0F))
            .texOffs(298, 82).addBox(16.5F, -15F, -18.5F, 2F, 19F, 2F, new CubeDeformation(0F))
            .texOffs(307, 82).addBox(16.5F, -15F, 16.5F, 2F, 19F, 2F, new CubeDeformation(0F))
            .texOffs(314, 112).addBox(-9F, -8F, -19F, 18F, 11F, 4F, new CubeDeformation(0F))
            .texOffs(0, 177).addBox(-11F, -10F, -21F, 22F, 2F, 4F, new CubeDeformation(0F))
            .texOffs(331, 158).addBox(-9F, -14F, -19.5F, 2F, 7F, 2F, new CubeDeformation(0F))
            .texOffs(340, 158).addBox(7F, -14F, -19.5F, 2F, 7F, 2F, new CubeDeformation(0F))
            .texOffs(183, 169).addBox(-2.5F, -14F, -21F, 5F, 5F, 2F, new CubeDeformation(0F))
            .texOffs(399, 158).addBox(-21F, -5F, 1F, 4F, 4F, 4F, new CubeDeformation(0F))
            .texOffs(450, 158).addBox(-18F, -11F, -10F, 1F, 4F, 4F, new CubeDeformation(0F))
            .texOffs(17, 112).addBox(-23F, -14F, -18F, 8F, 8F, 8F, new CubeDeformation(0F))
            .texOffs(50, 112).addBox(-18F, -4F, -13F, 8F, 8F, 8F, new CubeDeformation(0F))
            .texOffs(83, 112).addBox(-18F, -4F, 5F, 8F, 8F, 8F, new CubeDeformation(0F))
            .texOffs(416, 158).addBox(17F, -5F, 1F, 4F, 4F, 4F, new CubeDeformation(0F))
            .texOffs(461, 158).addBox(17F, -11F, -10F, 1F, 4F, 4F, new CubeDeformation(0F))
            .texOffs(116, 112).addBox(15F, -14F, -18F, 8F, 8F, 8F, new CubeDeformation(0F))
            .texOffs(149, 112).addBox(10F, -4F, -13F, 8F, 8F, 8F, new CubeDeformation(0F))
            .texOffs(182, 112).addBox(10F, -4F, 5F, 8F, 8F, 8F, new CubeDeformation(0F))
            .texOffs(58, 82).addBox(-11F, -14F, 17F, 22F, 16F, 9F, new CubeDeformation(0F))
            .texOffs(289, 130).addBox(-12F, -2F, 17F, 24F, 2F, 10F, new CubeDeformation(0F))
            .texOffs(358, 130).addBox(-12F, -12F, 17F, 24F, 2F, 10F, new CubeDeformation(0F))
            .texOffs(457, 82).addBox(-9F, -28F, 19F, 4F, 14F, 4F, new CubeDeformation(0F))
            .texOffs(349, 158).addBox(-10F, -30F, 18F, 6F, 2F, 6F, new CubeDeformation(0F))
            .texOffs(231, 190).addBox(-8.5F, -31F, 19.5F, 3F, 1F, 3F, new CubeDeformation(0F))
            .texOffs(474, 82).addBox(5F, -28F, 19F, 4F, 14F, 4F, new CubeDeformation(0F))
            .texOffs(374, 158).addBox(4F, -30F, 18F, 6F, 2F, 6F, new CubeDeformation(0F))
            .texOffs(244, 190).addBox(5.5F, -31F, 19.5F, 3F, 1F, 3F, new CubeDeformation(0F)), PartPose.offset(0F, -6F, 0F));
        PartDefinition p_gear_lens_h = p_body.addOrReplaceChild("gear_lens_h", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -11.5F, -19.2F, 3.1416F, 0F, 3.1416F));
        PartDefinition p_gear_lens = p_gear_lens_h.addOrReplaceChild("gear_lens", CubeListBuilder.create()
            .texOffs(296, 177).addBox(-3.5F, -1.5F, 0F, 7F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(287, 158).addBox(-1.5F, -3.5F, 0F, 3F, 7F, 2F, new CubeDeformation(0.03F))
            .texOffs(198, 169).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 2F, new CubeDeformation(0.06F))
            .texOffs(257, 190).addBox(3F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(266, 190).addBox(-5F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(275, 190).addBox(-1F, 3F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(284, 190).addBox(-1F, -5F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(293, 190).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(36, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_lens_t1 = p_gear_lens.addOrReplaceChild("gear_lens_t1", CubeListBuilder.create()
            .texOffs(302, 190).addBox(3F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(311, 190).addBox(-5F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(320, 190).addBox(-1F, 3F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(329, 190).addBox(-1F, -5F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_gear_grate_r_h = p_body.addOrReplaceChild("gear_grate_r_h", CubeListBuilder.create(), PartPose.offsetAndRotation(-12.5F, -2F, -19.3F, 3.1416F, 0F, 3.1416F));
        PartDefinition p_gear_grate_r = p_gear_grate_r_h.addOrReplaceChild("gear_grate_r", CubeListBuilder.create()
            .texOffs(372, 177).addBox(-2.5F, -1.5F, 0F, 5F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(336, 169).addBox(-1.5F, -2.5F, 0F, 3F, 5F, 2F, new CubeDeformation(0.03F))
            .texOffs(130, 184).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 2F, new CubeDeformation(0.06F))
            .texOffs(295, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(304, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(99, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(106, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(338, 190).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(41, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_grate_r_t1 = p_gear_grate_r.addOrReplaceChild("gear_grate_r_t1", CubeListBuilder.create()
            .texOffs(313, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(322, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(113, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(120, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_gear_side_r_h = p_body.addOrReplaceChild("gear_side_r_h", CubeListBuilder.create(), PartPose.offsetAndRotation(-21F, -3F, 3F, 0F, -1.5708F, 0F));
        PartDefinition p_gear_side_r = p_gear_side_r_h.addOrReplaceChild("gear_side_r", CubeListBuilder.create()
            .texOffs(310, 145).addBox(-7.5F, -3.5F, 0F, 15F, 7F, 3F, new CubeDeformation(0F))
            .texOffs(415, 82).addBox(-3.5F, -7.5F, 0F, 7F, 15F, 3F, new CubeDeformation(0.03F))
            .texOffs(359, 112).addBox(-5.5F, -5.5F, 0F, 11F, 11F, 3F, new CubeDeformation(0.06F))
            .texOffs(141, 184).addBox(7F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(152, 184).addBox(-9F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(163, 184).addBox(-1F, 7F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(174, 184).addBox(-1F, -9F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(72, 177).addBox(-2.5F, -2.5F, 3F, 5F, 5F, 1F, new CubeDeformation(0F))
            .texOffs(347, 190).addBox(-1.5F, -1.5F, 4F, 3F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_side_r_t1 = p_gear_side_r.addOrReplaceChild("gear_side_r_t1", CubeListBuilder.create()
            .texOffs(185, 184).addBox(7F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(196, 184).addBox(-9F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(207, 184).addBox(-1F, 7F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(218, 184).addBox(-1F, -9F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.3927F));
        PartDefinition p_gear_side_r_t2 = p_gear_side_r.addOrReplaceChild("gear_side_r_t2", CubeListBuilder.create()
            .texOffs(229, 184).addBox(7F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(240, 184).addBox(-9F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(251, 184).addBox(-1F, 7F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(262, 184).addBox(-1F, -9F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_gear_side_r_t3 = p_gear_side_r.addOrReplaceChild("gear_side_r_t3", CubeListBuilder.create()
            .texOffs(273, 184).addBox(7F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(284, 184).addBox(-9F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(295, 184).addBox(-1F, 7F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(306, 184).addBox(-1F, -9F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.1781F));
        PartDefinition p_gear_mesh_r_h = p_body.addOrReplaceChild("gear_mesh_r_h", CubeListBuilder.create(), PartPose.offsetAndRotation(-19F, -12F, -8F, 0F, -1.5708F, 0F));
        PartDefinition p_gear_mesh_r = p_gear_mesh_r_h.addOrReplaceChild("gear_mesh_r", CubeListBuilder.create()
            .texOffs(387, 177).addBox(-2.5F, -1.5F, 0F, 5F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(347, 169).addBox(-1.5F, -2.5F, 0F, 3F, 5F, 2F, new CubeDeformation(0.03F))
            .texOffs(317, 184).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 2F, new CubeDeformation(0.06F))
            .texOffs(331, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(340, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(127, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(134, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(356, 190).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(46, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_mesh_r_t1 = p_gear_mesh_r.addOrReplaceChild("gear_mesh_r_t1", CubeListBuilder.create()
            .texOffs(349, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(358, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(141, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(148, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_gear_sh_r_h = p_body.addOrReplaceChild("gear_sh_r_h", CubeListBuilder.create(), PartPose.offsetAndRotation(-23F, -10F, -14F, 0F, -1.5708F, 0F));
        PartDefinition p_gear_sh_r = p_gear_sh_r_h.addOrReplaceChild("gear_sh_r", CubeListBuilder.create()
            .texOffs(402, 177).addBox(-2.5F, -1.5F, 0F, 5F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(358, 169).addBox(-1.5F, -2.5F, 0F, 3F, 5F, 2F, new CubeDeformation(0.03F))
            .texOffs(328, 184).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 2F, new CubeDeformation(0.06F))
            .texOffs(367, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(376, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(155, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(162, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(365, 190).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(51, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_sh_r_t1 = p_gear_sh_r.addOrReplaceChild("gear_sh_r_t1", CubeListBuilder.create()
            .texOffs(385, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(394, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(169, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(176, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_gear_grate_l_h = p_body.addOrReplaceChild("gear_grate_l_h", CubeListBuilder.create(), PartPose.offsetAndRotation(12.5F, -2F, -19.3F, 3.1416F, 0F, 3.1416F));
        PartDefinition p_gear_grate_l = p_gear_grate_l_h.addOrReplaceChild("gear_grate_l", CubeListBuilder.create()
            .texOffs(417, 177).addBox(-2.5F, -1.5F, 0F, 5F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(369, 169).addBox(-1.5F, -2.5F, 0F, 3F, 5F, 2F, new CubeDeformation(0.03F))
            .texOffs(339, 184).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 2F, new CubeDeformation(0.06F))
            .texOffs(403, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(412, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(183, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(190, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(374, 190).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(56, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_grate_l_t1 = p_gear_grate_l.addOrReplaceChild("gear_grate_l_t1", CubeListBuilder.create()
            .texOffs(421, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(430, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(197, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(204, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_gear_side_l_h = p_body.addOrReplaceChild("gear_side_l_h", CubeListBuilder.create(), PartPose.offsetAndRotation(21F, -3F, 3F, 0F, 1.5708F, 0F));
        PartDefinition p_gear_side_l = p_gear_side_l_h.addOrReplaceChild("gear_side_l", CubeListBuilder.create()
            .texOffs(347, 145).addBox(-7.5F, -3.5F, 0F, 15F, 7F, 3F, new CubeDeformation(0F))
            .texOffs(436, 82).addBox(-3.5F, -7.5F, 0F, 7F, 15F, 3F, new CubeDeformation(0.03F))
            .texOffs(388, 112).addBox(-5.5F, -5.5F, 0F, 11F, 11F, 3F, new CubeDeformation(0.06F))
            .texOffs(350, 184).addBox(7F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(361, 184).addBox(-9F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(372, 184).addBox(-1F, 7F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(383, 184).addBox(-1F, -9F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(85, 177).addBox(-2.5F, -2.5F, 3F, 5F, 5F, 1F, new CubeDeformation(0F))
            .texOffs(383, 190).addBox(-1.5F, -1.5F, 4F, 3F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_side_l_t1 = p_gear_side_l.addOrReplaceChild("gear_side_l_t1", CubeListBuilder.create()
            .texOffs(394, 184).addBox(7F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(405, 184).addBox(-9F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(416, 184).addBox(-1F, 7F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(427, 184).addBox(-1F, -9F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.3927F));
        PartDefinition p_gear_side_l_t2 = p_gear_side_l.addOrReplaceChild("gear_side_l_t2", CubeListBuilder.create()
            .texOffs(438, 184).addBox(7F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(449, 184).addBox(-9F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(460, 184).addBox(-1F, 7F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(471, 184).addBox(-1F, -9F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_gear_side_l_t3 = p_gear_side_l.addOrReplaceChild("gear_side_l_t3", CubeListBuilder.create()
            .texOffs(482, 184).addBox(7F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(493, 184).addBox(-9F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(0, 190).addBox(-1F, 7F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(11, 190).addBox(-1F, -9F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.1781F));
        PartDefinition p_gear_mesh_l_h = p_body.addOrReplaceChild("gear_mesh_l_h", CubeListBuilder.create(), PartPose.offsetAndRotation(19F, -12F, -8F, 0F, 1.5708F, 0F));
        PartDefinition p_gear_mesh_l = p_gear_mesh_l_h.addOrReplaceChild("gear_mesh_l", CubeListBuilder.create()
            .texOffs(432, 177).addBox(-2.5F, -1.5F, 0F, 5F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(380, 169).addBox(-1.5F, -2.5F, 0F, 3F, 5F, 2F, new CubeDeformation(0.03F))
            .texOffs(22, 190).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 2F, new CubeDeformation(0.06F))
            .texOffs(439, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(448, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(211, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(218, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(392, 190).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(61, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_mesh_l_t1 = p_gear_mesh_l.addOrReplaceChild("gear_mesh_l_t1", CubeListBuilder.create()
            .texOffs(457, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(466, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(225, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(232, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_gear_sh_l_h = p_body.addOrReplaceChild("gear_sh_l_h", CubeListBuilder.create(), PartPose.offsetAndRotation(23F, -10F, -14F, 0F, 1.5708F, 0F));
        PartDefinition p_gear_sh_l = p_gear_sh_l_h.addOrReplaceChild("gear_sh_l", CubeListBuilder.create()
            .texOffs(447, 177).addBox(-2.5F, -1.5F, 0F, 5F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(391, 169).addBox(-1.5F, -2.5F, 0F, 3F, 5F, 2F, new CubeDeformation(0.03F))
            .texOffs(33, 190).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 2F, new CubeDeformation(0.06F))
            .texOffs(475, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(484, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(239, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(246, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(401, 190).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(66, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_sh_l_t1 = p_gear_sh_l.addOrReplaceChild("gear_sh_l_t1", CubeListBuilder.create()
            .texOffs(493, 201).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(502, 201).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(253, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(260, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_gear_back_a_h = p_body.addOrReplaceChild("gear_back_a_h", CubeListBuilder.create(), PartPose.offset(-4F, -5F, 26F));
        PartDefinition p_gear_back_a = p_gear_back_a_h.addOrReplaceChild("gear_back_a", CubeListBuilder.create()
            .texOffs(181, 177).addBox(-4.5F, -1.5F, 0F, 9F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(196, 145).addBox(-1.5F, -4.5F, 0F, 3F, 9F, 2F, new CubeDeformation(0.03F))
            .texOffs(36, 158).addBox(-3.5F, -3.5F, 0F, 7F, 7F, 2F, new CubeDeformation(0.06F))
            .texOffs(410, 190).addBox(4F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(419, 190).addBox(-6F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(428, 190).addBox(-1F, 4F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(437, 190).addBox(-1F, -6F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(446, 190).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(71, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_back_a_t1 = p_gear_back_a.addOrReplaceChild("gear_back_a_t1", CubeListBuilder.create()
            .texOffs(455, 190).addBox(4F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(464, 190).addBox(-6F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(473, 190).addBox(-1F, 4F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(482, 190).addBox(-1F, -6F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_gear_back_b_h = p_body.addOrReplaceChild("gear_back_b_h", CubeListBuilder.create(), PartPose.offset(6F, -7F, 26F));
        PartDefinition p_gear_back_b = p_gear_back_b_h.addOrReplaceChild("gear_back_b", CubeListBuilder.create()
            .texOffs(462, 177).addBox(-2.5F, -1.5F, 0F, 5F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(402, 169).addBox(-1.5F, -2.5F, 0F, 3F, 5F, 2F, new CubeDeformation(0.03F))
            .texOffs(44, 190).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 2F, new CubeDeformation(0.06F))
            .texOffs(0, 206).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(9, 206).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(267, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(274, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(491, 190).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(76, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_gear_back_b_t1 = p_gear_back_b.addOrReplaceChild("gear_back_b_t1", CubeListBuilder.create()
            .texOffs(18, 206).addBox(2F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(27, 206).addBox(-4F, -0.5F, 0F, 2F, 1F, 2F, new CubeDeformation(0.09F))
            .texOffs(281, 201).addBox(-0.5F, 2F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(288, 201).addBox(-0.5F, -4F, 0F, 1F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_body_rod1 = p_body.addOrReplaceChild("body_rod1", CubeListBuilder.create()
            .texOffs(203, 130).addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-18F, -18F, -18F, -0.5465F, -1.4057F, 3.1416F));
        PartDefinition p_body_rod2 = p_body.addOrReplaceChild("body_rod2", CubeListBuilder.create()
            .texOffs(163, 177).addBox(-1F, 0F, -1F, 2F, 4F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-22.8F, -26F, -18.8F, 2.3299F, -0.3217F, 0F));
        PartDefinition p_body_rod3 = p_body.addOrReplaceChild("body_rod3", CubeListBuilder.create()
            .texOffs(491, 82).addBox(-2F, 0F, -2F, 4F, 13F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-18F, -18F, 18F, 2.2444F, -0.4994F, 0F));
        PartDefinition p_body_rod4 = p_body.addOrReplaceChild("body_rod4", CubeListBuilder.create()
            .texOffs(472, 158).addBox(-1F, 0F, -1F, 2F, 6F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-22.8F, -26F, 26.8F, 2.1026F, -0.1974F, 0F));
        PartDefinition p_body_rod5 = p_body.addOrReplaceChild("body_rod5", CubeListBuilder.create()
            .texOffs(220, 130).addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(18F, -18F, -18F, -0.5465F, 1.4057F, 3.1416F));
        PartDefinition p_body_rod6 = p_body.addOrReplaceChild("body_rod6", CubeListBuilder.create()
            .texOffs(172, 177).addBox(-1F, 0F, -1F, 2F, 4F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(22.8F, -26F, -18.8F, 2.3299F, 0.3217F, 0F));
        PartDefinition p_body_rod7 = p_body.addOrReplaceChild("body_rod7", CubeListBuilder.create()
            .texOffs(0, 112).addBox(-2F, 0F, -2F, 4F, 13F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(18F, -18F, 18F, 2.2444F, 0.4994F, 0F));
        PartDefinition p_body_rod8 = p_body.addOrReplaceChild("body_rod8", CubeListBuilder.create()
            .texOffs(481, 158).addBox(-1F, 0F, -1F, 2F, 6F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(22.8F, -26F, 26.8F, 2.1026F, 0.1974F, 0F));
        PartDefinition p_cage = p_body.addOrReplaceChild("cage", CubeListBuilder.create()
            .texOffs(271, 45).addBox(-1.5F, -17.5F, -15F, 3F, 3F, 30F, new CubeDeformation(0F)), PartPose.offset(0F, -20F, 0F));
        PartDefinition p_cage_rod1 = p_cage.addOrReplaceChild("cage_rod1", CubeListBuilder.create()
            .texOffs(134, 82).addBox(-1.5F, 0F, -1.5F, 3F, 21F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-14F, 0F, -14F, 2.4228F, 1.5708F, 0F));
        PartDefinition p_cage_rod2 = p_cage.addOrReplaceChild("cage_rod2", CubeListBuilder.create()
            .texOffs(147, 82).addBox(-1.5F, 0F, -1.5F, 3F, 21F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(14F, 0F, -14F, 2.4228F, -1.5708F, 0F));
        PartDefinition p_cage_rod3 = p_cage.addOrReplaceChild("cage_rod3", CubeListBuilder.create()
            .texOffs(160, 82).addBox(-1.5F, 0F, -1.5F, 3F, 21F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-14F, 0F, 14F, 2.4228F, 1.5708F, 0F));
        PartDefinition p_cage_rod4 = p_cage.addOrReplaceChild("cage_rod4", CubeListBuilder.create()
            .texOffs(173, 82).addBox(-1.5F, 0F, -1.5F, 3F, 21F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(14F, 0F, 14F, 2.4228F, -1.5708F, 0F));
        PartDefinition p_cage_rod5 = p_cage.addOrReplaceChild("cage_rod5", CubeListBuilder.create()
            .texOffs(228, 82).addBox(-1.5F, 0F, -1.5F, 3F, 18F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-14F, 0F, -14F, 2.2794F, 0F, 0F));
        PartDefinition p_cage_rod6 = p_cage.addOrReplaceChild("cage_rod6", CubeListBuilder.create()
            .texOffs(241, 82).addBox(-1.5F, 0F, -1.5F, 3F, 18F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-14F, 0F, 14F, -0.8622F, 0F, 3.1416F));
        PartDefinition p_cage_rod7 = p_cage.addOrReplaceChild("cage_rod7", CubeListBuilder.create()
            .texOffs(254, 82).addBox(-1.5F, 0F, -1.5F, 3F, 18F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(14F, 0F, -14F, 2.2794F, 0F, 0F));
        PartDefinition p_cage_rod8 = p_cage.addOrReplaceChild("cage_rod8", CubeListBuilder.create()
            .texOffs(267, 82).addBox(-1.5F, 0F, -1.5F, 3F, 18F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(14F, 0F, 14F, -0.8622F, 0F, 3.1416F));
        PartDefinition p_turntable_h = p_cage.addOrReplaceChild("turntable_h", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -3F, 0F, 1.5708F, 0F, 0F));
        PartDefinition p_turntable = p_turntable_h.addOrReplaceChild("turntable", CubeListBuilder.create()
            .texOffs(269, 145).addBox(-8.5F, -3.5F, 0F, 17F, 7F, 3F, new CubeDeformation(0F))
            .texOffs(316, 82).addBox(-3.5F, -8.5F, 0F, 7F, 17F, 3F, new CubeDeformation(0.03F))
            .texOffs(215, 112).addBox(-6.5F, -6.5F, 0F, 13F, 13F, 3F, new CubeDeformation(0.06F))
            .texOffs(55, 190).addBox(8F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(66, 190).addBox(-10F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(77, 190).addBox(-1F, 8F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(88, 190).addBox(-1F, -10F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(433, 158).addBox(-3.5F, -3.5F, 3F, 7F, 7F, 1F, new CubeDeformation(0F))
            .texOffs(500, 190).addBox(-1.5F, -1.5F, 4F, 3F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_turntable_t1 = p_turntable.addOrReplaceChild("turntable_t1", CubeListBuilder.create()
            .texOffs(99, 190).addBox(8F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(110, 190).addBox(-10F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(121, 190).addBox(-1F, 8F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(132, 190).addBox(-1F, -10F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.3927F));
        PartDefinition p_turntable_t2 = p_turntable.addOrReplaceChild("turntable_t2", CubeListBuilder.create()
            .texOffs(143, 190).addBox(8F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(154, 190).addBox(-10F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(165, 190).addBox(-1F, 8F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(176, 190).addBox(-1F, -10F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_turntable_t3 = p_turntable.addOrReplaceChild("turntable_t3", CubeListBuilder.create()
            .texOffs(187, 190).addBox(8F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(198, 190).addBox(-10F, -1F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(209, 190).addBox(-1F, 8F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F))
            .texOffs(220, 190).addBox(-1F, -10F, 0F, 2F, 2F, 3F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.1781F));
        PartDefinition p_turntable_top_h = p_cage.addOrReplaceChild("turntable_top_h", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -8F, 0F, 1.5708F, 0F, 0F));
        PartDefinition p_turntable_top = p_turntable_top_h.addOrReplaceChild("turntable_top", CubeListBuilder.create()
            .texOffs(315, 177).addBox(-3.5F, -1.5F, 0F, 7F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(298, 158).addBox(-1.5F, -3.5F, 0F, 3F, 7F, 2F, new CubeDeformation(0.03F))
            .texOffs(213, 169).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 2F, new CubeDeformation(0.06F))
            .texOffs(0, 196).addBox(3F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(9, 196).addBox(-5F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(18, 196).addBox(-1F, 3F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(27, 196).addBox(-1F, -5F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(36, 196).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(81, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_turntable_top_t1 = p_turntable_top.addOrReplaceChild("turntable_top_t1", CubeListBuilder.create()
            .texOffs(45, 196).addBox(3F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(54, 196).addBox(-5F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(63, 196).addBox(-1F, 3F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(72, 196).addBox(-1F, -5F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_governor = p_cage.addOrReplaceChild("governor", CubeListBuilder.create()
            .texOffs(121, 82).addBox(-1.5F, -17F, -1.5F, 3F, 22F, 3F, new CubeDeformation(0F))
            .texOffs(384, 145).addBox(-9.5F, -10.5F, -2.5F, 5F, 5F, 5F, new CubeDeformation(0F))
            .texOffs(405, 145).addBox(4.5F, -10.5F, -2.5F, 5F, 5F, 5F, new CubeDeformation(0F))
            .texOffs(166, 169).addBox(-2F, -20F, -2F, 4F, 3F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, -10F, 0F));
        PartDefinition p_governor_rod1 = p_governor.addOrReplaceChild("governor_rod1", CubeListBuilder.create()
            .texOffs(74, 145).addBox(-1F, 0F, -1F, 2F, 10F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -15F, 0F, 0.7854F, -1.5708F, 0F));
        PartDefinition p_governor_rod2 = p_governor.addOrReplaceChild("governor_rod2", CubeListBuilder.create()
            .texOffs(83, 145).addBox(-1F, 0F, -1F, 2F, 10F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -15F, 0F, 0.7854F, 1.5708F, 0F));
        PartDefinition p_ring = p_cage.addOrReplaceChild("ring", CubeListBuilder.create()
            .texOffs(98, 177).addBox(22.5F, -1.5F, -1.5F, 3F, 3F, 3F, new CubeDeformation(0F))
            .texOffs(81, 196).addBox(18.4164F, -2.9021F, 13.1068F, 2F, 2F, 2F, new CubeDeformation(0F))
            .texOffs(111, 177).addBox(5.9164F, -2.6756F, 21.3254F, 3F, 3F, 3F, new CubeDeformation(0F))
            .texOffs(90, 196).addBox(-8.4164F, 0.1756F, 21.8254F, 2F, 2F, 2F, new CubeDeformation(0F))
            .texOffs(124, 177).addBox(-20.9164F, 0.4021F, 12.6068F, 3F, 3F, 3F, new CubeDeformation(0F))
            .texOffs(99, 196).addBox(-25F, -1F, -1F, 2F, 2F, 2F, new CubeDeformation(0F))
            .texOffs(137, 177).addBox(-20.9164F, -3.4021F, -15.6068F, 3F, 3F, 3F, new CubeDeformation(0F))
            .texOffs(108, 196).addBox(-8.4164F, -2.1756F, -23.8254F, 2F, 2F, 2F, new CubeDeformation(0F))
            .texOffs(150, 177).addBox(5.9164F, -0.3244F, -24.3254F, 3F, 3F, 3F, new CubeDeformation(0F))
            .texOffs(117, 196).addBox(18.4164F, 0.9021F, -15.1068F, 2F, 2F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, -18F, 0F));
        PartDefinition p_thigh_fr = p_body.addOrReplaceChild("thigh_fr", CubeListBuilder.create()
            .texOffs(431, 45).addBox(-3.5F, -1F, -3.5F, 7F, 22F, 7F, new CubeDeformation(0F))
            .texOffs(92, 145).addBox(-1.5F, 3F, 3.5F, 3F, 8F, 3F, new CubeDeformation(0F))
            .texOffs(478, 145).addBox(-1F, 11F, 3.5F, 2F, 8F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-14F, 0F, -9F, -0.9748F, -0.8254F, 3.1416F));
        PartDefinition p_shin_fr = p_thigh_fr.addOrReplaceChild("shin_fr", CubeListBuilder.create()
            .texOffs(417, 112).addBox(-3.5F, -3.5F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0F))
            .texOffs(411, 0).addBox(-3F, 3F, -3F, 6F, 32F, 6F, new CubeDeformation(0F))
            .texOffs(237, 130).addBox(-1.3F, 4F, 3F, 3F, 10F, 3F, new CubeDeformation(0F))
            .texOffs(379, 82).addBox(-1F, 14F, 3F, 2F, 17F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 21.3776F, 0F, -1.9447F, 0.1324F, 0.1976F));
        PartDefinition p_kneelvl_fr = p_shin_fr.addOrReplaceChild("kneelvl_fr", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, -2.892F, 0.5736F, -3.0042F));
        PartDefinition p_knee_fr_h = p_kneelvl_fr.addOrReplaceChild("knee_fr_h", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.5F, 0F, 0F, 0F, -1.5708F, 0F));
        PartDefinition p_knee_fr = p_knee_fr_h.addOrReplaceChild("knee_fr", CubeListBuilder.create()
            .texOffs(204, 177).addBox(-4.5F, -1.5F, 0F, 9F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(207, 145).addBox(-1.5F, -4.5F, 0F, 3F, 9F, 2F, new CubeDeformation(0.03F))
            .texOffs(55, 158).addBox(-3.5F, -3.5F, 0F, 7F, 7F, 2F, new CubeDeformation(0.06F))
            .texOffs(126, 196).addBox(4F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(135, 196).addBox(-6F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(144, 196).addBox(-1F, 4F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(153, 196).addBox(-1F, -6F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(162, 196).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(86, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_knee_fr_t1 = p_knee_fr.addOrReplaceChild("knee_fr_t1", CubeListBuilder.create()
            .texOffs(171, 196).addBox(4F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(180, 196).addBox(-6F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(189, 196).addBox(-1F, 4F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(198, 196).addBox(-1F, -6F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_foot_fr = p_shin_fr.addOrReplaceChild("foot_fr", CubeListBuilder.create()
            .texOffs(427, 130).addBox(-4.5F, -3F, -4.5F, 9F, 3F, 9F, new CubeDeformation(0F))
            .texOffs(87, 130).addBox(-3.5F, 0F, -3.5F, 7F, 6F, 7F, new CubeDeformation(0F))
            .texOffs(477, 177).addBox(-10.25F, 6F, -4F, 3F, 2F, 3F, new CubeDeformation(0F))
            .texOffs(490, 177).addBox(-0.25F, 6F, -11.5F, 3F, 2F, 3F, new CubeDeformation(0F))
            .texOffs(0, 184).addBox(-4F, 6F, 6F, 3F, 2F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 34.7563F, 0F, -2.892F, 0.5736F, -3.0042F));
        PartDefinition p_foot_fr_rod1 = p_foot_fr.addOrReplaceChild("foot_fr_rod1", CubeListBuilder.create()
            .texOffs(426, 145).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, -1.7073F, -1.2925F, 3.1416F));
        PartDefinition p_foot_fr_rod2 = p_foot_fr.addOrReplaceChild("foot_fr_rod2", CubeListBuilder.create()
            .texOffs(105, 145).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, -1.6942F, 0.1244F, 3.1416F));
        PartDefinition p_foot_fr_rod3 = p_foot_fr.addOrReplaceChild("foot_fr_rod3", CubeListBuilder.create()
            .texOffs(131, 158).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, 1.414F, -0.3217F, 0F));
        PartDefinition p_thigh_fl = p_body.addOrReplaceChild("thigh_fl", CubeListBuilder.create()
            .texOffs(460, 45).addBox(-3.5F, -1F, -3.5F, 7F, 22F, 7F, new CubeDeformation(0F))
            .texOffs(118, 145).addBox(-1.5F, 3F, 3.5F, 3F, 8F, 3F, new CubeDeformation(0F))
            .texOffs(487, 145).addBox(-1F, 11F, 3.5F, 2F, 8F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(14F, 0F, -9F, -0.9748F, 0.8254F, 3.1416F));
        PartDefinition p_shin_fl = p_thigh_fl.addOrReplaceChild("shin_fl", CubeListBuilder.create()
            .texOffs(446, 112).addBox(-3.5F, -3.5F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0F))
            .texOffs(436, 0).addBox(-3F, 3F, -3F, 6F, 32F, 6F, new CubeDeformation(0F))
            .texOffs(250, 130).addBox(-1.3F, 4F, 3F, 3F, 10F, 3F, new CubeDeformation(0F))
            .texOffs(388, 82).addBox(-1F, 14F, 3F, 2F, 17F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 21.3776F, 0F, -1.9447F, -0.1324F, -0.1976F));
        PartDefinition p_kneelvl_fl = p_shin_fl.addOrReplaceChild("kneelvl_fl", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, -2.892F, -0.5736F, 3.0042F));
        PartDefinition p_knee_fl_h = p_kneelvl_fl.addOrReplaceChild("knee_fl_h", CubeListBuilder.create(), PartPose.offsetAndRotation(3.5F, 0F, 0F, 0F, 1.5708F, 0F));
        PartDefinition p_knee_fl = p_knee_fl_h.addOrReplaceChild("knee_fl", CubeListBuilder.create()
            .texOffs(227, 177).addBox(-4.5F, -1.5F, 0F, 9F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(218, 145).addBox(-1.5F, -4.5F, 0F, 3F, 9F, 2F, new CubeDeformation(0.03F))
            .texOffs(74, 158).addBox(-3.5F, -3.5F, 0F, 7F, 7F, 2F, new CubeDeformation(0.06F))
            .texOffs(207, 196).addBox(4F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(216, 196).addBox(-6F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(225, 196).addBox(-1F, 4F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(234, 196).addBox(-1F, -6F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(243, 196).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(91, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_knee_fl_t1 = p_knee_fl.addOrReplaceChild("knee_fl_t1", CubeListBuilder.create()
            .texOffs(252, 196).addBox(4F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(261, 196).addBox(-6F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(270, 196).addBox(-1F, 4F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(279, 196).addBox(-1F, -6F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_foot_fl = p_shin_fl.addOrReplaceChild("foot_fl", CubeListBuilder.create()
            .texOffs(464, 130).addBox(-4.5F, -3F, -4.5F, 9F, 3F, 9F, new CubeDeformation(0F))
            .texOffs(116, 130).addBox(-3.5F, 0F, -3.5F, 7F, 6F, 7F, new CubeDeformation(0F))
            .texOffs(13, 184).addBox(7.25F, 6F, -4F, 3F, 2F, 3F, new CubeDeformation(0F))
            .texOffs(26, 184).addBox(-2.75F, 6F, -11.5F, 3F, 2F, 3F, new CubeDeformation(0F))
            .texOffs(39, 184).addBox(1F, 6F, 6F, 3F, 2F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 34.7563F, 0F, -2.892F, -0.5736F, 3.0042F));
        PartDefinition p_foot_fl_rod1 = p_foot_fl.addOrReplaceChild("foot_fl_rod1", CubeListBuilder.create()
            .texOffs(439, 145).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, -1.7073F, 1.2925F, 3.1416F));
        PartDefinition p_foot_fl_rod2 = p_foot_fl.addOrReplaceChild("foot_fl_rod2", CubeListBuilder.create()
            .texOffs(131, 145).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, -1.6942F, -0.1244F, 3.1416F));
        PartDefinition p_foot_fl_rod3 = p_foot_fl.addOrReplaceChild("foot_fl_rod3", CubeListBuilder.create()
            .texOffs(144, 158).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, 1.414F, 0.3217F, 0F));
        PartDefinition p_thigh_br = p_body.addOrReplaceChild("thigh_br", CubeListBuilder.create()
            .texOffs(0, 82).addBox(-3.5F, -1F, -3.5F, 7F, 22F, 7F, new CubeDeformation(0F))
            .texOffs(144, 145).addBox(-1.5F, 3F, 3.5F, 3F, 8F, 3F, new CubeDeformation(0F))
            .texOffs(496, 145).addBox(-1F, 11F, 3.5F, 2F, 8F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-14F, 0F, 9F, 2.1668F, -0.8254F, 0F));
        PartDefinition p_shin_br = p_thigh_br.addOrReplaceChild("shin_br", CubeListBuilder.create()
            .texOffs(475, 112).addBox(-3.5F, -3.5F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0F))
            .texOffs(461, 0).addBox(-3F, 3F, -3F, 6F, 32F, 6F, new CubeDeformation(0F))
            .texOffs(263, 130).addBox(-1.3F, 4F, 3F, 3F, 10F, 3F, new CubeDeformation(0F))
            .texOffs(397, 82).addBox(-1F, 14F, 3F, 2F, 17F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 21.3776F, 0F, -1.9447F, -0.1324F, -0.1976F));
        PartDefinition p_kneelvl_br = p_shin_br.addOrReplaceChild("kneelvl_br", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, -0.2496F, 0.5736F, -0.1374F));
        PartDefinition p_knee_br_h = p_kneelvl_br.addOrReplaceChild("knee_br_h", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.5F, 0F, 0F, 0F, -1.5708F, 0F));
        PartDefinition p_knee_br = p_knee_br_h.addOrReplaceChild("knee_br", CubeListBuilder.create()
            .texOffs(250, 177).addBox(-4.5F, -1.5F, 0F, 9F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(229, 145).addBox(-1.5F, -4.5F, 0F, 3F, 9F, 2F, new CubeDeformation(0.03F))
            .texOffs(93, 158).addBox(-3.5F, -3.5F, 0F, 7F, 7F, 2F, new CubeDeformation(0.06F))
            .texOffs(288, 196).addBox(4F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(297, 196).addBox(-6F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(306, 196).addBox(-1F, 4F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(315, 196).addBox(-1F, -6F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(324, 196).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(96, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_knee_br_t1 = p_knee_br.addOrReplaceChild("knee_br_t1", CubeListBuilder.create()
            .texOffs(333, 196).addBox(4F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(342, 196).addBox(-6F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(351, 196).addBox(-1F, 4F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(360, 196).addBox(-1F, -6F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_foot_br = p_shin_br.addOrReplaceChild("foot_br", CubeListBuilder.create()
            .texOffs(0, 145).addBox(-4.5F, -3F, -4.5F, 9F, 3F, 9F, new CubeDeformation(0F))
            .texOffs(145, 130).addBox(-3.5F, 0F, -3.5F, 7F, 6F, 7F, new CubeDeformation(0F))
            .texOffs(52, 184).addBox(-10.25F, 6F, 1F, 3F, 2F, 3F, new CubeDeformation(0F))
            .texOffs(65, 184).addBox(-0.25F, 6F, 8.5F, 3F, 2F, 3F, new CubeDeformation(0F))
            .texOffs(78, 184).addBox(-4F, 6F, -9F, 3F, 2F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 34.7563F, 0F, -0.2496F, 0.5736F, -0.1374F));
        PartDefinition p_foot_br_rod1 = p_foot_br.addOrReplaceChild("foot_br_rod1", CubeListBuilder.create()
            .texOffs(452, 145).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, 1.4343F, -1.2925F, 0F));
        PartDefinition p_foot_br_rod2 = p_foot_br.addOrReplaceChild("foot_br_rod2", CubeListBuilder.create()
            .texOffs(157, 145).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, 1.4474F, 0.1244F, 0F));
        PartDefinition p_foot_br_rod3 = p_foot_br.addOrReplaceChild("foot_br_rod3", CubeListBuilder.create()
            .texOffs(157, 158).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, -1.7276F, -0.3217F, 3.1416F));
        PartDefinition p_thigh_bl = p_body.addOrReplaceChild("thigh_bl", CubeListBuilder.create()
            .texOffs(29, 82).addBox(-3.5F, -1F, -3.5F, 7F, 22F, 7F, new CubeDeformation(0F))
            .texOffs(170, 145).addBox(-1.5F, 3F, 3.5F, 3F, 8F, 3F, new CubeDeformation(0F))
            .texOffs(0, 158).addBox(-1F, 11F, 3.5F, 2F, 8F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(14F, 0F, 9F, 2.1668F, 0.8254F, 0F));
        PartDefinition p_shin_bl = p_thigh_bl.addOrReplaceChild("shin_bl", CubeListBuilder.create()
            .texOffs(0, 130).addBox(-3.5F, -3.5F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0F))
            .texOffs(486, 0).addBox(-3F, 3F, -3F, 6F, 32F, 6F, new CubeDeformation(0F))
            .texOffs(276, 130).addBox(-1.3F, 4F, 3F, 3F, 10F, 3F, new CubeDeformation(0F))
            .texOffs(406, 82).addBox(-1F, 14F, 3F, 2F, 17F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 21.3776F, 0F, -1.9447F, 0.1324F, 0.1976F));
        PartDefinition p_kneelvl_bl = p_shin_bl.addOrReplaceChild("kneelvl_bl", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, -0.2496F, -0.5736F, 0.1374F));
        PartDefinition p_knee_bl_h = p_kneelvl_bl.addOrReplaceChild("knee_bl_h", CubeListBuilder.create(), PartPose.offsetAndRotation(3.5F, 0F, 0F, 0F, 1.5708F, 0F));
        PartDefinition p_knee_bl = p_knee_bl_h.addOrReplaceChild("knee_bl", CubeListBuilder.create()
            .texOffs(273, 177).addBox(-4.5F, -1.5F, 0F, 9F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(240, 145).addBox(-1.5F, -4.5F, 0F, 3F, 9F, 2F, new CubeDeformation(0.03F))
            .texOffs(112, 158).addBox(-3.5F, -3.5F, 0F, 7F, 7F, 2F, new CubeDeformation(0.06F))
            .texOffs(369, 196).addBox(4F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(378, 196).addBox(-6F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(387, 196).addBox(-1F, 4F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(396, 196).addBox(-1F, -6F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(405, 196).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(101, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_knee_bl_t1 = p_knee_bl.addOrReplaceChild("knee_bl_t1", CubeListBuilder.create()
            .texOffs(414, 196).addBox(4F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(423, 196).addBox(-6F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(432, 196).addBox(-1F, 4F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(441, 196).addBox(-1F, -6F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_foot_bl = p_shin_bl.addOrReplaceChild("foot_bl", CubeListBuilder.create()
            .texOffs(37, 145).addBox(-4.5F, -3F, -4.5F, 9F, 3F, 9F, new CubeDeformation(0F))
            .texOffs(174, 130).addBox(-3.5F, 0F, -3.5F, 7F, 6F, 7F, new CubeDeformation(0F))
            .texOffs(91, 184).addBox(7.25F, 6F, 1F, 3F, 2F, 3F, new CubeDeformation(0F))
            .texOffs(104, 184).addBox(-2.75F, 6F, 8.5F, 3F, 2F, 3F, new CubeDeformation(0F))
            .texOffs(117, 184).addBox(1F, 6F, -9F, 3F, 2F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 34.7563F, 0F, -0.2496F, -0.5736F, 0.1374F));
        PartDefinition p_foot_bl_rod1 = p_foot_bl.addOrReplaceChild("foot_bl_rod1", CubeListBuilder.create()
            .texOffs(465, 145).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, 1.4343F, 1.2925F, 0F));
        PartDefinition p_foot_bl_rod2 = p_foot_bl.addOrReplaceChild("foot_bl_rod2", CubeListBuilder.create()
            .texOffs(183, 145).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, 1.4474F, -0.1244F, 0F));
        PartDefinition p_foot_bl_rod3 = p_foot_bl.addOrReplaceChild("foot_bl_rod3", CubeListBuilder.create()
            .texOffs(170, 158).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.4F, 0F, -1.7276F, 0.3217F, 3.1416F));
        PartDefinition p_arm_r = p_body.addOrReplaceChild("arm_r", CubeListBuilder.create()
            .texOffs(186, 82).addBox(-2.5F, 0F, -2.5F, 5F, 18F, 5F, new CubeDeformation(0F))
            .texOffs(183, 158).addBox(-1.5F, 2F, 2.5F, 3F, 6F, 3F, new CubeDeformation(0F))
            .texOffs(251, 145).addBox(-1F, 8F, 2.5F, 2F, 9F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-19F, -10F, -14F, -1.6848F, -0.4124F, 3.1416F));
        PartDefinition p_fore_r = p_arm_r.addOrReplaceChild("fore_r", CubeListBuilder.create()
            .texOffs(29, 130).addBox(-3.5F, -3.5F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0F))
            .texOffs(337, 82).addBox(-2.5F, 3F, -2.5F, 5F, 14F, 5F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 17.5784F, 0F, -0.5565F, -0.092F, 0.9374F));
        PartDefinition p_elbowlvl_r = p_fore_r.addOrReplaceChild("elbowlvl_r", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, -2.1271F, -0.2988F, 2.6994F));
        PartDefinition p_egear_r_h = p_elbowlvl_r.addOrReplaceChild("egear_r_h", CubeListBuilder.create(), PartPose.offsetAndRotation(-3F, 0F, 0F, 0F, -1.5708F, 0F));
        PartDefinition p_egear_r = p_egear_r_h.addOrReplaceChild("egear_r", CubeListBuilder.create()
            .texOffs(334, 177).addBox(-3.5F, -1.5F, 0F, 7F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(309, 158).addBox(-1.5F, -3.5F, 0F, 3F, 7F, 2F, new CubeDeformation(0.03F))
            .texOffs(228, 169).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 2F, new CubeDeformation(0.06F))
            .texOffs(450, 196).addBox(3F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(459, 196).addBox(-5F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(468, 196).addBox(-1F, 3F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(477, 196).addBox(-1F, -5F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(486, 196).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(106, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_egear_r_t1 = p_egear_r.addOrReplaceChild("egear_r_t1", CubeListBuilder.create()
            .texOffs(495, 196).addBox(3F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(504, 196).addBox(-5F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(0, 201).addBox(-1F, 3F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(9, 201).addBox(-1F, -5F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_claw_r = p_fore_r.addOrReplaceChild("claw_r", CubeListBuilder.create()
            .texOffs(248, 112).addBox(-4F, -4F, -5F, 8F, 8F, 8F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 17.1172F, 0F, -2.1271F, -0.2988F, 2.6994F));
        PartDefinition p_claw_r_rod1 = p_claw_r.addOrReplaceChild("claw_r_rod1", CubeListBuilder.create()
            .texOffs(258, 169).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.5F, -3F, -4F, -1.423F, -0.1244F, 3.1416F));
        PartDefinition p_claw_r_rod2 = p_claw_r.addOrReplaceChild("claw_r_rod2", CubeListBuilder.create()
            .texOffs(196, 158).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-3F, -3.6F, -8F, -1.2384F, -0.1651F, 3.1416F));
        PartDefinition p_claw_r_rod3 = p_claw_r.addOrReplaceChild("claw_r_rod3", CubeListBuilder.create()
            .texOffs(413, 169).addBox(-1F, 0F, -1F, 2F, 5F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-4F, -5.7F, -14F, -2.1506F, 0.245F, 3.1416F));
        PartDefinition p_claw_r_rod4 = p_claw_r.addOrReplaceChild("claw_r_rod4", CubeListBuilder.create()
            .texOffs(271, 169).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.5F, -3F, -4F, -1.423F, 0.1244F, 3.1416F));
        PartDefinition p_claw_r_rod5 = p_claw_r.addOrReplaceChild("claw_r_rod5", CubeListBuilder.create()
            .texOffs(209, 158).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(3F, -3.6F, -8F, -1.2384F, 0.1651F, 3.1416F));
        PartDefinition p_claw_r_rod6 = p_claw_r.addOrReplaceChild("claw_r_rod6", CubeListBuilder.create()
            .texOffs(422, 169).addBox(-1F, 0F, -1F, 2F, 5F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(4F, -5.7F, -14F, -2.1506F, -0.245F, 3.1416F));
        PartDefinition p_claw_r_rod7 = p_claw_r.addOrReplaceChild("claw_r_rod7", CubeListBuilder.create()
            .texOffs(284, 169).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 3.5F, -4F, -1.744F, 0F, 3.1416F));
        PartDefinition p_claw_r_rod8 = p_claw_r.addOrReplaceChild("claw_r_rod8", CubeListBuilder.create()
            .texOffs(222, 158).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 4.2F, -8F, -1.9585F, 0F, 3.1416F));
        PartDefinition p_claw_r_rod9 = p_claw_r.addOrReplaceChild("claw_r_rod9", CubeListBuilder.create()
            .texOffs(431, 169).addBox(-1F, 0F, -1F, 2F, 5F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 6.65F, -14F, -0.9037F, 0F, 3.1416F));
        PartDefinition p_arm_l = p_body.addOrReplaceChild("arm_l", CubeListBuilder.create()
            .texOffs(207, 82).addBox(-2.5F, 0F, -2.5F, 5F, 18F, 5F, new CubeDeformation(0F))
            .texOffs(235, 158).addBox(-1.5F, 2F, 2.5F, 3F, 6F, 3F, new CubeDeformation(0F))
            .texOffs(260, 145).addBox(-1F, 8F, 2.5F, 2F, 9F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(19F, -10F, -14F, -1.6848F, 0.4124F, 3.1416F));
        PartDefinition p_fore_l = p_arm_l.addOrReplaceChild("fore_l", CubeListBuilder.create()
            .texOffs(58, 130).addBox(-3.5F, -3.5F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0F))
            .texOffs(358, 82).addBox(-2.5F, 3F, -2.5F, 5F, 14F, 5F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 17.5784F, 0F, -0.5565F, 0.092F, -0.9374F));
        PartDefinition p_elbowlvl_l = p_fore_l.addOrReplaceChild("elbowlvl_l", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, -2.1271F, 0.2988F, -2.6994F));
        PartDefinition p_egear_l_h = p_elbowlvl_l.addOrReplaceChild("egear_l_h", CubeListBuilder.create(), PartPose.offsetAndRotation(3F, 0F, 0F, 0F, 1.5708F, 0F));
        PartDefinition p_egear_l = p_egear_l_h.addOrReplaceChild("egear_l", CubeListBuilder.create()
            .texOffs(353, 177).addBox(-3.5F, -1.5F, 0F, 7F, 3F, 2F, new CubeDeformation(0F))
            .texOffs(320, 158).addBox(-1.5F, -3.5F, 0F, 3F, 7F, 2F, new CubeDeformation(0.03F))
            .texOffs(243, 169).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 2F, new CubeDeformation(0.06F))
            .texOffs(18, 201).addBox(3F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(27, 201).addBox(-5F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(36, 201).addBox(-1F, 3F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(45, 201).addBox(-1F, -5F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(54, 201).addBox(-1.5F, -1.5F, 2F, 3F, 3F, 1F, new CubeDeformation(0F))
            .texOffs(111, 206).addBox(-0.5F, -0.5F, 3F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_egear_l_t1 = p_egear_l.addOrReplaceChild("egear_l_t1", CubeListBuilder.create()
            .texOffs(63, 201).addBox(3F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(72, 201).addBox(-5F, -1F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(81, 201).addBox(-1F, 3F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F))
            .texOffs(90, 201).addBox(-1F, -5F, 0F, 2F, 2F, 2F, new CubeDeformation(0.09F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        PartDefinition p_claw_l = p_fore_l.addOrReplaceChild("claw_l", CubeListBuilder.create()
            .texOffs(281, 112).addBox(-4F, -4F, -5F, 8F, 8F, 8F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 17.1172F, 0F, -2.1271F, 0.2988F, -2.6994F));
        PartDefinition p_claw_l_rod1 = p_claw_l.addOrReplaceChild("claw_l_rod1", CubeListBuilder.create()
            .texOffs(297, 169).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.5F, -3F, -4F, -1.423F, -0.1244F, 3.1416F));
        PartDefinition p_claw_l_rod2 = p_claw_l.addOrReplaceChild("claw_l_rod2", CubeListBuilder.create()
            .texOffs(248, 158).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-3F, -3.6F, -8F, -1.2384F, -0.1651F, 3.1416F));
        PartDefinition p_claw_l_rod3 = p_claw_l.addOrReplaceChild("claw_l_rod3", CubeListBuilder.create()
            .texOffs(440, 169).addBox(-1F, 0F, -1F, 2F, 5F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-4F, -5.7F, -14F, -2.1506F, 0.245F, 3.1416F));
        PartDefinition p_claw_l_rod4 = p_claw_l.addOrReplaceChild("claw_l_rod4", CubeListBuilder.create()
            .texOffs(310, 169).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.5F, -3F, -4F, -1.423F, 0.1244F, 3.1416F));
        PartDefinition p_claw_l_rod5 = p_claw_l.addOrReplaceChild("claw_l_rod5", CubeListBuilder.create()
            .texOffs(261, 158).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(3F, -3.6F, -8F, -1.2384F, 0.1651F, 3.1416F));
        PartDefinition p_claw_l_rod6 = p_claw_l.addOrReplaceChild("claw_l_rod6", CubeListBuilder.create()
            .texOffs(449, 169).addBox(-1F, 0F, -1F, 2F, 5F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(4F, -5.7F, -14F, -2.1506F, -0.245F, 3.1416F));
        PartDefinition p_claw_l_rod7 = p_claw_l.addOrReplaceChild("claw_l_rod7", CubeListBuilder.create()
            .texOffs(323, 169).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 3.5F, -4F, -1.744F, 0F, 3.1416F));
        PartDefinition p_claw_l_rod8 = p_claw_l.addOrReplaceChild("claw_l_rod8", CubeListBuilder.create()
            .texOffs(274, 158).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 4.2F, -8F, -1.9585F, 0F, 3.1416F));
        PartDefinition p_claw_l_rod9 = p_claw_l.addOrReplaceChild("claw_l_rod9", CubeListBuilder.create()
            .texOffs(458, 169).addBox(-1F, 0F, -1F, 2F, 5F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 6.65F, -14F, -0.9037F, 0F, 3.1416F));
        return LayerDefinition.create(mesh, 512, 512);
    }

    private CrucibleWalkerLayer() {
    }
}
