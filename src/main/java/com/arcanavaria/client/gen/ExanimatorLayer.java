package com.arcanavaria.client.gen;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Written by tools/bossmodels/export_exanimator.py from the same spec that paints the texture. Edit the spec, not this file. */
public final class ExanimatorLayer {
    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(39, 129).addBox(-4.9F, -8.4F, -4.2F, 10F, 11F, 8F, new CubeDeformation(0F)), PartPose.offset(0F, -4F, 0F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create()
            .texOffs(422, 0).addBox(-7.7F, 0F, -6.3F, 15F, 10F, 13F, new CubeDeformation(0F))
            .texOffs(199, 0).addBox(-11.2F, 9.8F, -8.4F, 22F, 7F, 17F, new CubeDeformation(0F)), PartPose.offset(0F, 2.8F, 0F));
        PartDefinition p_chest = p_body.addOrReplaceChild("chest", CubeListBuilder.create()
            .texOffs(361, 0).addBox(-8.4F, -9.8F, -6.3F, 17F, 10F, 13F, new CubeDeformation(0F))
            .texOffs(116, 0).addBox(-13.3F, -22.4F, -7F, 27F, 13F, 14F, new CubeDeformation(0F))
            .texOffs(388, 105).addBox(-1.4F, -21F, -9.24F, 3F, 18F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -8.4F, 0F, 0.2F, 0F, 0F));
        PartDefinition p_rib0l = p_chest.addOrReplaceChild("rib0l", CubeListBuilder.create()
            .texOffs(124, 180).addBox(0F, -0.84F, -0.84F, 12F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0.84F, -19.6F, -8.12F, 0F, -0.18F, 0.22F));
        PartDefinition p_rib0lb = p_rib0l.addOrReplaceChild("rib0lb", CubeListBuilder.create()
            .texOffs(262, 180).addBox(0F, -0.84F, -0.84F, 6F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(11.48F, 0F, 0F, 0F, -1.05F, 0.25F));
        PartDefinition p_rib0r = p_chest.addOrReplaceChild("rib0r", CubeListBuilder.create()
            .texOffs(153, 180).addBox(-12.04F, -0.84F, -0.84F, 12F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-0.84F, -19.6F, -8.12F, 0F, 0.18F, -0.22F));
        PartDefinition p_rib0rb = p_rib0r.addOrReplaceChild("rib0rb", CubeListBuilder.create()
            .texOffs(279, 180).addBox(-5.6F, -0.84F, -0.84F, 6F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-11.48F, 0F, 0F, 0F, 1.05F, -0.25F));
        PartDefinition p_rib1l = p_chest.addOrReplaceChild("rib1l", CubeListBuilder.create()
            .texOffs(0, 180).addBox(0F, -0.84F, -0.84F, 13F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0.84F, -15.96F, -8.12F, 0F, -0.18F, 0.22F));
        PartDefinition p_rib1lb = p_rib1l.addOrReplaceChild("rib1lb", CubeListBuilder.create()
            .texOffs(296, 180).addBox(0F, -0.84F, -0.84F, 6F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(12.04F, 0F, 0F, 0F, -1.05F, 0.25F));
        PartDefinition p_rib1r = p_chest.addOrReplaceChild("rib1r", CubeListBuilder.create()
            .texOffs(31, 180).addBox(-12.6F, -0.84F, -0.84F, 13F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-0.84F, -15.96F, -8.12F, 0F, 0.18F, -0.22F));
        PartDefinition p_rib1rb = p_rib1r.addOrReplaceChild("rib1rb", CubeListBuilder.create()
            .texOffs(313, 180).addBox(-6.3F, -0.84F, -0.84F, 6F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-12.04F, 0F, 0F, 0F, 1.05F, -0.25F));
        PartDefinition p_rib2l = p_chest.addOrReplaceChild("rib2l", CubeListBuilder.create()
            .texOffs(62, 180).addBox(0F, -0.84F, -0.84F, 13F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0.84F, -12.32F, -8.12F, 0F, -0.18F, 0.22F));
        PartDefinition p_rib2lb = p_rib2l.addOrReplaceChild("rib2lb", CubeListBuilder.create()
            .texOffs(330, 180).addBox(0F, -0.84F, -0.84F, 6F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(12.04F, 0F, 0F, 0F, -1.05F, 0.25F));
        PartDefinition p_rib2r = p_chest.addOrReplaceChild("rib2r", CubeListBuilder.create()
            .texOffs(93, 180).addBox(-12.6F, -0.84F, -0.84F, 13F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-0.84F, -12.32F, -8.12F, 0F, 0.18F, -0.22F));
        PartDefinition p_rib2rb = p_rib2r.addOrReplaceChild("rib2rb", CubeListBuilder.create()
            .texOffs(347, 180).addBox(-6.3F, -0.84F, -0.84F, 6F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-12.04F, 0F, 0F, 0F, 1.05F, -0.25F));
        PartDefinition p_rib3l = p_chest.addOrReplaceChild("rib3l", CubeListBuilder.create()
            .texOffs(182, 180).addBox(0F, -0.84F, -0.84F, 8F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0.84F, -7.84F, -7.42F, 0F, -0.18F, 0.22F));
        PartDefinition p_rib3lb = p_rib3l.addOrReplaceChild("rib3lb", CubeListBuilder.create()
            .texOffs(364, 180).addBox(0F, -0.84F, -0.84F, 6F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(7.56F, 0F, 0F, 0F, -1.05F, 0.25F));
        PartDefinition p_rib3r = p_chest.addOrReplaceChild("rib3r", CubeListBuilder.create()
            .texOffs(203, 180).addBox(-8.12F, -0.84F, -0.84F, 8F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-0.84F, -7.84F, -7.42F, 0F, 0.18F, -0.22F));
        PartDefinition p_rib3rb = p_rib3r.addOrReplaceChild("rib3rb", CubeListBuilder.create()
            .texOffs(381, 180).addBox(-5.6F, -0.84F, -0.84F, 6F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-7.56F, 0F, 0F, 0F, 1.05F, -0.25F));
        PartDefinition p_rib4l = p_chest.addOrReplaceChild("rib4l", CubeListBuilder.create()
            .texOffs(224, 180).addBox(0F, -0.84F, -0.84F, 7F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0.84F, -4.2F, -7.14F, 0F, -0.18F, 0.22F));
        PartDefinition p_rib4lb = p_rib4l.addOrReplaceChild("rib4lb", CubeListBuilder.create()
            .texOffs(398, 180).addBox(0F, -0.84F, -0.84F, 5F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(6.72F, 0F, 0F, 0F, -1.05F, 0.25F));
        PartDefinition p_rib4r = p_chest.addOrReplaceChild("rib4r", CubeListBuilder.create()
            .texOffs(243, 180).addBox(-7.28F, -0.84F, -0.84F, 7F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-0.84F, -4.2F, -7.14F, 0F, 0.18F, -0.22F));
        PartDefinition p_rib4rb = p_rib4r.addOrReplaceChild("rib4rb", CubeListBuilder.create()
            .texOffs(413, 180).addBox(-4.9F, -0.84F, -0.84F, 5F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-6.72F, 0F, 0F, 0F, 1.05F, -0.25F));
        PartDefinition p_hump_l = p_chest.addOrReplaceChild("hump_l", CubeListBuilder.create()
            .texOffs(0, 105).addBox(-4.9F, -4.9F, -6.3F, 10F, 10F, 13F, new CubeDeformation(0F))
            .texOffs(125, 167).addBox(0F, -14.7F, 0F, 10F, 11F, 0F, new CubeDeformation(0F)), PartPose.offset(14.7F, -23.1F, 0.7F));
        PartDefinition p_hump_r = p_chest.addOrReplaceChild("hump_r", CubeListBuilder.create()
            .texOffs(47, 105).addBox(-4.9F, -4.9F, -6.3F, 10F, 10F, 13F, new CubeDeformation(0F))
            .texOffs(146, 167).addBox(-9.8F, -14.7F, 0F, 10F, 11F, 0F, new CubeDeformation(0F)), PartPose.offset(-14.7F, -23.1F, 0.7F));
        PartDefinition p_head = p_chest.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(312, 167).addBox(-2.1F, -5.6F, -2.1F, 4F, 6F, 4F, new CubeDeformation(0F))
            .texOffs(50, 167).addBox(-3.5F, -9.8F, -4.2F, 7F, 4F, 7F, new CubeDeformation(0F))
            .texOffs(293, 105).addBox(-4.9F, -23.8F, -4.9F, 10F, 14F, 8F, new CubeDeformation(0F))
            .texOffs(180, 167).addBox(-2.8F, -28F, -4.2F, 6F, 4F, 6F, new CubeDeformation(0F))
            .texOffs(13, 0).addBox(-18.9F, -36.4F, 4.9F, 38F, 38F, 0F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -19.6F, -2.8F, -0.16F, 0F, 0F));
        PartDefinition p_arm_lo_l = p_chest.addOrReplaceChild("arm_lo_l", CubeListBuilder.create()
            .texOffs(327, 0).addBox(-2.1F, -1.4F, -2.1F, 4F, 20F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(11.9F, -16.8F, -1.4F, 0F, 0F, -0.5984F));
        PartDefinition p_fore_lo_l = p_arm_lo_l.addOrReplaceChild("fore_lo_l", CubeListBuilder.create()
            .texOffs(90, 0).addBox(-1.4F, -1.4F, -1.4F, 3F, 27F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 18.6389F, 0F, 0F, 0F, 0.6539F));
        PartDefinition p_hand_lo_l = p_fore_lo_l.addOrReplaceChild("hand_lo_l", CubeListBuilder.create()
            .texOffs(79, 167).addBox(-5.6F, -1.4F, 0F, 11F, 11F, 0F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 25.2389F, 0F, 0F, 0F, -0.0555F));
        PartDefinition p_arm_lo_r = p_chest.addOrReplaceChild("arm_lo_r", CubeListBuilder.create()
            .texOffs(344, 0).addBox(-2.1F, -1.4F, -2.1F, 4F, 20F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-11.9F, -16.8F, -1.4F, 0F, 0F, 0.5984F));
        PartDefinition p_fore_lo_r = p_arm_lo_r.addOrReplaceChild("fore_lo_r", CubeListBuilder.create()
            .texOffs(103, 0).addBox(-1.4F, -1.4F, -1.4F, 3F, 27F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 18.6389F, 0F, 0F, 0F, -0.6539F));
        PartDefinition p_hand_lo_r = p_fore_lo_r.addOrReplaceChild("hand_lo_r", CubeListBuilder.create()
            .texOffs(102, 167).addBox(-5.6F, -1.4F, 0F, 11F, 11F, 0F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 25.2389F, 0F, 0F, 0F, 0.0555F));
        PartDefinition p_arm_hi_r = p_chest.addOrReplaceChild("arm_hi_r", CubeListBuilder.create()
            .texOffs(330, 105).addBox(-2.1F, -1.4F, -2.1F, 4F, 17F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-11.9F, -21F, -1.4F, 0F, 0F, 1.9348F));
        PartDefinition p_fore_hi_r = p_arm_hi_r.addOrReplaceChild("fore_hi_r", CubeListBuilder.create()
            .texOffs(70, 149).addBox(-1.4F, -1.4F, -1.4F, 3F, 14F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 15.7305F, 0F, 0F, 0F, 0.7432F));
        PartDefinition p_hand_hi_r = p_fore_hi_r.addOrReplaceChild("hand_hi_r", CubeListBuilder.create()
            .texOffs(205, 167).addBox(-5.6F, -1.4F, 0F, 11F, 10F, 0F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 12.522F, 0F, 0F, 0F, 0.4636F));
        PartDefinition p_arm_hi_l = p_chest.addOrReplaceChild("arm_hi_l", CubeListBuilder.create()
            .texOffs(76, 129).addBox(-2.1F, -1.4F, -2.1F, 4F, 15F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(11.9F, -21F, -1.4F, 0F, 0F, -2.2455F));
        PartDefinition p_fore_hi_l = p_arm_hi_l.addOrReplaceChild("fore_hi_l", CubeListBuilder.create()
            .texOffs(167, 167).addBox(-1.4F, -1.4F, -1.4F, 3F, 8F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 13.4466F, 0F, 0F, 0F, 1.7819F));
        PartDefinition p_scythe = p_body.addOrReplaceChild("scythe", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-1.4F, -37.8F, -1.4F, 3F, 101F, 3F, new CubeDeformation(0F))
            .texOffs(329, 167).addBox(-2.1F, -2.8F, -2.1F, 4F, 6F, 4F, new CubeDeformation(0F))
            .texOffs(141, 105).addBox(-51.8F, -46.2F, 0F, 53F, 22F, 0F, new CubeDeformation(0F)), PartPose.offset(25.2F, -32.2F, -2.8F));
        PartDefinition p_tent0_0 = p_skirt.addOrReplaceChild("tent0_0", CubeListBuilder.create()
            .texOffs(278, 129).addBox(-4.2F, -4.2F, -8.75F, 8F, 8F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(7.4021F, 13.3F, -5.2933F, 0.6F, -0.95F, 0F));
        PartDefinition p_tent0_1 = p_tent0_0.addOrReplaceChild("tent0_1", CubeListBuilder.create()
            .texOffs(93, 129).addBox(-3.5F, -3.5F, -10.36F, 7F, 7F, 11F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.05F, -0.25F, -0.3037F, 0F));
        PartDefinition p_tent0_2 = p_tent0_1.addOrReplaceChild("tent0_2", CubeListBuilder.create()
            .texOffs(313, 129).addBox(-3.01F, -3.01F, -10.36F, 6F, 6F, 11F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -9.66F, -0.3F, -0.1915F, 0F));
        PartDefinition p_tent0_3 = p_tent0_2.addOrReplaceChild("tent0_3", CubeListBuilder.create()
            .texOffs(244, 149).addBox(-2.45F, -2.45F, -8.75F, 5F, 5F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -9.66F, -0.3F, 0.1829F, 0F));
        PartDefinition p_tent0_4 = p_tent0_3.addOrReplaceChild("tent0_4", CubeListBuilder.create()
            .texOffs(360, 149).addBox(-1.75F, -1.75F, -8.75F, 4F, 4F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.05F, -0.4F, 0.3069F, 0F));
        PartDefinition p_tent0_5 = p_tent0_4.addOrReplaceChild("tent0_5", CubeListBuilder.create()
            .texOffs(228, 167).addBox(-1.05F, -1.05F, -7.14F, 2F, 2F, 8F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.05F, -0.6F, 0.0106F, 0F));
        PartDefinition p_tent0_6 = p_tent0_5.addOrReplaceChild("tent0_6", CubeListBuilder.create()
            .texOffs(403, 167).addBox(-0.7F, -0.7F, -5.53F, 1F, 1F, 6F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -6.44F, -0.85F, -0.3002F, 0F));
        PartDefinition p_tent1_0 = p_skirt.addOrReplaceChild("tent1_0", CubeListBuilder.create()
            .texOffs(348, 129).addBox(-4.2F, -4.2F, -8.4F, 8F, 8F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-7.4021F, 13.3F, -5.2933F, 0.6F, 0.95F, 0F));
        PartDefinition p_tent1_1 = p_tent1_0.addOrReplaceChild("tent1_1", CubeListBuilder.create()
            .texOffs(130, 129).addBox(-3.5F, -3.5F, -9.94F, 7F, 7F, 11F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -7.7F, -0.25F, 0.2678F, 0F));
        PartDefinition p_tent1_2 = p_tent1_1.addOrReplaceChild("tent1_2", CubeListBuilder.create()
            .texOffs(383, 129).addBox(-3.01F, -3.01F, -9.94F, 6F, 6F, 11F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -9.24F, -0.3F, -0.0818F, 0F));
        PartDefinition p_tent1_3 = p_tent1_2.addOrReplaceChild("tent1_3", CubeListBuilder.create()
            .texOffs(273, 149).addBox(-2.45F, -2.45F, -8.4F, 5F, 5F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -9.24F, -0.42F, -0.3194F, 0F));
        PartDefinition p_tent1_4 = p_tent1_3.addOrReplaceChild("tent1_4", CubeListBuilder.create()
            .texOffs(387, 149).addBox(-1.75F, -1.75F, -8.4F, 4F, 4F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -7.7F, -0.52F, -0.1196F, 0F));
        PartDefinition p_tent1_5 = p_tent1_4.addOrReplaceChild("tent1_5", CubeListBuilder.create()
            .texOffs(249, 167).addBox(-1.05F, -1.05F, -6.86F, 2F, 2F, 8F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -7.7F, -0.72F, 0.2439F, 0F));
        PartDefinition p_tent1_6 = p_tent1_5.addOrReplaceChild("tent1_6", CubeListBuilder.create()
            .texOffs(418, 167).addBox(-0.7F, -0.7F, -5.32F, 1F, 1F, 6F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -6.16F, -0.97F, 0.2735F, 0F));
        PartDefinition p_tent2_0 = p_skirt.addOrReplaceChild("tent2_0", CubeListBuilder.create()
            .texOffs(418, 129).addBox(-4.2F, -4.2F, -8.75F, 8F, 8F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-7.3573F, 13.3F, 5.3554F, 0.6F, 2.2F, 0F));
        PartDefinition p_tent2_1 = p_tent2_0.addOrReplaceChild("tent2_1", CubeListBuilder.create()
            .texOffs(167, 129).addBox(-3.5F, -3.5F, -10.36F, 7F, 7F, 11F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.05F, -0.25F, -0.0293F, 0F));
        PartDefinition p_tent2_2 = p_tent2_1.addOrReplaceChild("tent2_2", CubeListBuilder.create()
            .texOffs(453, 129).addBox(-3.01F, -3.01F, -10.36F, 6F, 6F, 11F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -9.66F, -0.3F, 0.2932F, 0F));
        PartDefinition p_tent2_3 = p_tent2_2.addOrReplaceChild("tent2_3", CubeListBuilder.create()
            .texOffs(302, 149).addBox(-2.45F, -2.45F, -8.75F, 5F, 5F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -9.66F, -0.3F, 0.2142F, 0F));
        PartDefinition p_tent2_4 = p_tent2_3.addOrReplaceChild("tent2_4", CubeListBuilder.create()
            .texOffs(414, 149).addBox(-1.75F, -1.75F, -8.75F, 4F, 4F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.05F, -0.4F, -0.1581F, 0F));
        PartDefinition p_tent2_5 = p_tent2_4.addOrReplaceChild("tent2_5", CubeListBuilder.create()
            .texOffs(270, 167).addBox(-1.05F, -1.05F, -7.14F, 2F, 2F, 8F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.05F, -0.6F, -0.3139F, 0F));
        PartDefinition p_tent2_6 = p_tent2_5.addOrReplaceChild("tent2_6", CubeListBuilder.create()
            .texOffs(433, 167).addBox(-0.7F, -0.7F, -5.53F, 1F, 1F, 6F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -6.44F, -0.85F, -0.0398F, 0F));
        PartDefinition p_tent3_0 = p_skirt.addOrReplaceChild("tent3_0", CubeListBuilder.create()
            .texOffs(0, 149).addBox(-4.2F, -4.2F, -8.4F, 8F, 8F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(7.3573F, 13.3F, 5.3554F, 0.6F, -2.2F, 0F));
        PartDefinition p_tent3_1 = p_tent3_0.addOrReplaceChild("tent3_1", CubeListBuilder.create()
            .texOffs(204, 129).addBox(-3.5F, -3.5F, -9.94F, 7F, 7F, 11F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -7.7F, -0.25F, -0.2314F, 0F));
        PartDefinition p_tent3_2 = p_tent3_1.addOrReplaceChild("tent3_2", CubeListBuilder.create()
            .texOffs(35, 149).addBox(-3.01F, -3.01F, -9.94F, 6F, 6F, 11F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -9.24F, -0.3F, -0.2827F, 0F));
        PartDefinition p_tent3_3 = p_tent3_2.addOrReplaceChild("tent3_3", CubeListBuilder.create()
            .texOffs(331, 149).addBox(-2.45F, -2.45F, -8.4F, 5F, 5F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -9.24F, -0.3F, 0.0531F, 0F));
        PartDefinition p_tent3_4 = p_tent3_3.addOrReplaceChild("tent3_4", CubeListBuilder.create()
            .texOffs(441, 149).addBox(-1.75F, -1.75F, -8.4F, 4F, 4F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -7.7F, -0.4F, 0.3162F, 0F));
        PartDefinition p_tent3_5 = p_tent3_4.addOrReplaceChild("tent3_5", CubeListBuilder.create()
            .texOffs(291, 167).addBox(-1.05F, -1.05F, -6.86F, 2F, 2F, 8F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -7.7F, -0.6F, 0.1463F, 0F));
        PartDefinition p_tent3_6 = p_tent3_5.addOrReplaceChild("tent3_6", CubeListBuilder.create()
            .texOffs(448, 167).addBox(-0.7F, -0.7F, -5.32F, 1F, 1F, 6F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -6.16F, -0.85F, -0.224F, 0F));
        PartDefinition p_tent4_0 = p_chest.addOrReplaceChild("tent4_0", CubeListBuilder.create()
            .texOffs(278, 0).addBox(-4.2F, -4.2F, -15.4F, 8F, 8F, 16F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -9.8F, 6.3F, -1.3F, 3.1416F, 0F));
        PartDefinition p_tent4_1 = p_tent4_0.addOrReplaceChild("tent4_1", CubeListBuilder.create()
            .texOffs(94, 105).addBox(-3.5F, -3.5F, -15.4F, 7F, 7F, 16F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -14.7F, 0.05F, 0.317F, 0F));
        PartDefinition p_tent4_2 = p_tent4_1.addOrReplaceChild("tent4_2", CubeListBuilder.create()
            .texOffs(248, 105).addBox(-3.01F, -3.01F, -15.4F, 6F, 6F, 16F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -14.7F, -0.15F, 0.0583F, 0F));
        PartDefinition p_tent4_3 = p_tent4_2.addOrReplaceChild("tent4_3", CubeListBuilder.create()
            .texOffs(399, 105).addBox(-2.45F, -2.45F, -13.3F, 5F, 5F, 14F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -14.7F, -0.32F, -0.2802F, 0F));
        PartDefinition p_tent4_4 = p_tent4_3.addOrReplaceChild("tent4_4", CubeListBuilder.create()
            .texOffs(83, 149).addBox(-1.75F, -1.75F, -11.2F, 4F, 4F, 12F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -12.6F, -0.47F, -0.235F, 0F));
        PartDefinition p_tent4_5 = p_tent4_4.addOrReplaceChild("tent4_5", CubeListBuilder.create()
            .texOffs(0, 167).addBox(-1.05F, -1.05F, -9.1F, 2F, 2F, 10F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -10.5F, -0.72F, 0.132F, 0F));
        PartDefinition p_tent4_6 = p_tent4_5.addOrReplaceChild("tent4_6", CubeListBuilder.create()
            .texOffs(346, 167).addBox(-0.7F, -0.7F, -7F, 1F, 1F, 8F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.4F, -1.07F, 0.3183F, 0F));
        PartDefinition p_tent5_0 = p_chest.addOrReplaceChild("tent5_0", CubeListBuilder.create()
            .texOffs(438, 105).addBox(-4.2F, -4.2F, -10.5F, 8F, 8F, 11F, new CubeDeformation(0F)), PartPose.offsetAndRotation(6.3F, -15.4F, 6.3F, -0.3F, 4.1916F, 0F));
        PartDefinition p_tent5_1 = p_tent5_0.addOrReplaceChild("tent5_1", CubeListBuilder.create()
            .texOffs(241, 129).addBox(-3.5F, -3.5F, -10.5F, 7F, 7F, 11F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -9.8F, -0.15F, -0.1626F, 0F));
        PartDefinition p_tent5_2 = p_tent5_1.addOrReplaceChild("tent5_2", CubeListBuilder.create()
            .texOffs(116, 149).addBox(-3.01F, -3.01F, -9.1F, 6F, 6F, 10F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -9.8F, -0.25F, 0.2102F, 0F));
        PartDefinition p_tent5_3 = p_tent5_2.addOrReplaceChild("tent5_3", CubeListBuilder.create()
            .texOffs(182, 149).addBox(-2.45F, -2.45F, -9.1F, 5F, 5F, 10F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.4F, -0.3F, 0.2952F, 0F));
        PartDefinition p_tent5_4 = p_tent5_3.addOrReplaceChild("tent5_4", CubeListBuilder.create()
            .texOffs(25, 167).addBox(-1.75F, -1.75F, -7.7F, 4F, 4F, 8F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.4F, -0.4F, -0.024F, 0F));
        PartDefinition p_tent5_5 = p_tent5_4.addOrReplaceChild("tent5_5", CubeListBuilder.create()
            .texOffs(365, 167).addBox(-1.05F, -1.05F, -6.3F, 2F, 2F, 7F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -7F, -0.65F, -0.3104F, 0F));
        PartDefinition p_tent5_6 = p_tent5_5.addOrReplaceChild("tent5_6", CubeListBuilder.create()
            .texOffs(463, 167).addBox(-0.7F, -0.7F, -4.9F, 1F, 1F, 6F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -5.6F, -0.9F, -0.1717F, 0F));
        PartDefinition p_tent6_0 = p_chest.addOrReplaceChild("tent6_0", CubeListBuilder.create()
            .texOffs(347, 105).addBox(-4.2F, -4.2F, -10.99F, 8F, 8F, 12F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-6.3F, -15.4F, 6.3F, -0.3F, 2.0916F, 0F));
        PartDefinition p_tent6_1 = p_tent6_0.addOrReplaceChild("tent6_1", CubeListBuilder.create()
            .texOffs(0, 129).addBox(-3.5F, -3.5F, -10.99F, 7F, 7F, 12F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -10.29F, -0.15F, -0.1148F, 0F));
        PartDefinition p_tent6_2 = p_tent6_1.addOrReplaceChild("tent6_2", CubeListBuilder.create()
            .texOffs(149, 149).addBox(-3.01F, -3.01F, -9.52F, 6F, 6F, 10F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -10.29F, -0.25F, -0.3197F, 0F));
        PartDefinition p_tent6_3 = p_tent6_2.addOrReplaceChild("tent6_3", CubeListBuilder.create()
            .texOffs(213, 149).addBox(-2.45F, -2.45F, -9.52F, 5F, 5F, 10F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.82F, -0.3F, -0.0868F, 0F));
        PartDefinition p_tent6_4 = p_tent6_3.addOrReplaceChild("tent6_4", CubeListBuilder.create()
            .texOffs(468, 149).addBox(-1.75F, -1.75F, -8.05F, 4F, 4F, 9F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -8.82F, -0.4F, 0.2649F, 0F));
        PartDefinition p_tent6_5 = p_tent6_4.addOrReplaceChild("tent6_5", CubeListBuilder.create()
            .texOffs(384, 167).addBox(-1.05F, -1.05F, -6.58F, 2F, 2F, 7F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -7.35F, -0.65F, 0.2539F, 0F));
        PartDefinition p_tent6_6 = p_tent6_5.addOrReplaceChild("tent6_6", CubeListBuilder.create()
            .texOffs(478, 167).addBox(-0.7F, -0.7F, -5.11F, 1F, 1F, 6F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, -5.88F, -0.9F, -0.1048F, 0F));
        return LayerDefinition.create(mesh, 512, 256);
    }

    private ExanimatorLayer() {
    }
}
