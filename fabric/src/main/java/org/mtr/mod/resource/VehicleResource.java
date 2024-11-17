package org.mtr.mod.resource;

import org.mtr.core.data.TransportMode;
import org.mtr.core.serializer.ReaderBase;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.*;
import org.mtr.mapping.holder.Box;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.Init;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.generated.resource.VehicleResourceSchema;
import org.mtr.mod.render.DynamicVehicleModel;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.sound.BveVehicleSound;
import org.mtr.mod.sound.BveVehicleSoundConfig;
import org.mtr.mod.sound.LegacyVehicleSound;
import org.mtr.mod.sound.VehicleSoundBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class VehicleResource extends VehicleResourceSchema {

	public final Supplier<VehicleSoundBase> createVehicleSoundBase;
	public final CachedResource<VehicleResourceCache> cachedVehicleResource;

	private static final boolean[][] CHRISTMAS_LIGHT_STAGES = {
			{true, false, false, false},
			{false, true, false, false},
			{false, false, true, false},
			{false, false, false, true},
			{true, false, false, false},
			{false, true, false, false},
			{false, false, true, false},
			{false, false, false, true},

			{true, true, false, false},
			{false, true, true, false},
			{false, false, true, true},
			{true, false, false, true},
			{true, true, false, false},
			{false, true, true, false},
			{false, false, true, true},
			{true, false, false, true},

			{true, false, true, false},
			{false, true, false, true},
			{true, false, true, false},
			{false, true, false, true},
			{true, false, true, false},
			{false, true, false, true},
			{true, false, true, false},
			{false, true, false, true},

			{true, false, false, false},
			{true, true, false, false},
			{true, true, true, false},
			{true, true, true, true},
			{false, true, false, false},
			{false, true, true, false},
			{false, true, true, true},
			{true, true, true, true},
			{false, false, true, false},
			{false, false, true, true},
			{true, false, true, true},
			{true, true, true, true},
			{false, false, false, true},
			{true, false, false, true},
			{true, true, false, true},
			{true, true, true, true},

			{false, false, false, false},
			{true, true, true, true},
			{true, true, true, true},
			{true, true, true, true},
			{false, false, false, false},
			{true, true, true, true},
			{true, true, true, true},
			{true, true, true, true},
	};

	public VehicleResource(ReaderBase readerBase, @Nullable ObjectArrayList<VehicleModel> extraModels, ResourceProvider resourceProvider) {
		super(readerBase, resourceProvider);
		updateData(readerBase);

		if (extraModels != null) {
			models.addAll(extraModels);
		}

		cachedVehicleResource = cachedVehicleResourceInitializer();
		createVehicleSoundBase = createVehicleSoundBaseInitializer();
	}

	public VehicleResource(ReaderBase readerBase, ResourceProvider resourceProvider) {
		this(readerBase, null, resourceProvider);
	}

	VehicleResource(
			String id,
			String name,
			String color,
			TransportMode transportMode,
			double length,
			double width,
			double bogie1Position,
			double bogie2Position,
			double couplingPadding1,
			double couplingPadding2,
			String description,
			String wikipediaArticle,
			ObjectArrayList<String> tags,
			ObjectArrayList<VehicleModel> models,
			ObjectArrayList<VehicleModel> bogie1Models,
			ObjectArrayList<VehicleModel> bogie2Models,
			boolean hasGangway1,
			boolean hasGangway2,
			boolean hasBarrier1,
			boolean hasBarrier2,
			double legacyRiderOffset,
			String bveSoundBaseResource,
			String legacySpeedSoundBaseResource,
			long legacySpeedSoundCount,
			boolean legacyUseAccelerationSoundsWhenCoasting,
			boolean legacyConstantPlaybackSpeed,
			String legacyDoorSoundBaseResource,
			double legacyDoorCloseSoundTime,
			ResourceProvider resourceProvider
	) {
		super(
				id,
				name,
				color,
				transportMode,
				length,
				width,
				bogie1Position,
				bogie2Position,
				couplingPadding1,
				couplingPadding2,
				description,
				wikipediaArticle,
				hasGangway1,
				hasGangway2,
				hasBarrier1,
				hasBarrier2,
				legacyRiderOffset,
				bveSoundBaseResource,
				legacySpeedSoundBaseResource,
				legacySpeedSoundCount,
				legacyUseAccelerationSoundsWhenCoasting,
				legacyConstantPlaybackSpeed,
				legacyDoorSoundBaseResource,
				legacyDoorCloseSoundTime,
				resourceProvider
		);
		this.tags.addAll(tags);
		this.models.addAll(models);
		this.bogie1Models.addAll(bogie1Models);
		this.bogie2Models.addAll(bogie2Models);
		this.cachedVehicleResource = cachedVehicleResourceInitializer();
		this.createVehicleSoundBase = createVehicleSoundBaseInitializer();
	}

	@Nonnull
	@Override
	protected ResourceProvider modelsResourceProviderParameter() {
		return resourceProvider;
	}

	@Nonnull
	@Override
	protected ResourceProvider bogie1ModelsResourceProviderParameter() {
		return resourceProvider;
	}

	@Nonnull
	@Override
	protected ResourceProvider bogie2ModelsResourceProviderParameter() {
		return resourceProvider;
	}

	public void queue(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int light, ObjectArrayList<Box> openDoorways) {
		final VehicleResourceCache vehicleResourceCache = cachedVehicleResource.getData(false);
		if (vehicleResourceCache != null) {
			if (openDoorways.isEmpty()) {
				queue(vehicleResourceCache.optimizedModelsDoorsClosed, storedMatrixTransformations, vehicle, light, true);
			} else {
				queue(vehicleResourceCache.optimizedModels, storedMatrixTransformations, vehicle, light, false);
			}
		}
	}

	public void queueBogie(int bogieIndex, StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int light) {
		final VehicleResourceCache vehicleResourceCache = cachedVehicleResource.getData(false);
		if (vehicleResourceCache != null && Utilities.isBetween(bogieIndex, 0, 1)) {
			queue(bogieIndex == 0 ? vehicleResourceCache.optimizedModelsBogie1 : vehicleResourceCache.optimizedModelsBogie2, storedMatrixTransformations, vehicle, light, true);
		}
	}

	public String getId() {
		return id;
	}

	public MutableText getName() {
		return TextHelper.translatable(name);
	}

	public int getColor() {
		return CustomResourceTools.colorStringToInt(color);
	}

	public TransportMode getTransportMode() {
		return transportMode;
	}

	public double getLength() {
		return length;
	}

	public double getWidth() {
		return width;
	}

	public double getBogie1Position() {
		return bogie1Position;
	}

	public double getBogie2Position() {
		return bogie2Position;
	}

	public double getCouplingPadding1() {
		return couplingPadding1;
	}

	public double getCouplingPadding2() {
		return couplingPadding2;
	}

	public MutableText getDescription() {
		return TextHelper.translatable(description);
	}

	public String getWikipediaArticle() {
		return wikipediaArticle;
	}

	public void iterateModels(ModelConsumer modelConsumer) {
		iterateModels(models, modelConsumer);
	}

	public void iterateBogieModels(int bogieIndex, ModelConsumer modelConsumer) {
		if (Utilities.isBetween(bogieIndex, 0, 1)) {
			iterateModels(bogieIndex == 0 ? bogie1Models : bogie2Models, modelConsumer);
		}
	}

	public VehicleResourceWrapper toVehicleResourceWrapper() {
		return new VehicleResourceWrapper(
				id,
				name,
				color,
				transportMode,
				length,
				width,
				bogie1Position,
				bogie2Position,
				couplingPadding1,
				couplingPadding2,
				description,
				wikipediaArticle,
				tags,
				models.stream().map(VehicleModel::toVehicleModelWrapper).collect(Collectors.toCollection(ObjectArrayList::new)),
				bogie1Models.stream().map(VehicleModel::toVehicleModelWrapper).collect(Collectors.toCollection(ObjectArrayList::new)),
				bogie2Models.stream().map(VehicleModel::toVehicleModelWrapper).collect(Collectors.toCollection(ObjectArrayList::new)),
				hasGangway1,
				hasGangway2,
				hasBarrier1,
				hasBarrier2,
				legacyRiderOffset,
				bveSoundBaseResource,
				legacySpeedSoundBaseResource,
				legacySpeedSoundCount,
				legacyUseAccelerationSoundsWhenCoasting,
				legacyConstantPlaybackSpeed,
				legacyDoorSoundBaseResource,
				legacyDoorCloseSoundTime
		);
	}

	public void writeMinecraftResource(ObjectArraySet<MinecraftModelResource> minecraftModelResources, ObjectArraySet<String> minecraftTextureResources) {
		models.forEach(vehicleModel -> {
			minecraftModelResources.add(vehicleModel.getAsMinecraftResource());
			vehicleModel.addToTextureResource(minecraftTextureResources);
		});
		bogie1Models.forEach(vehicleModel -> {
			minecraftModelResources.add(vehicleModel.getAsMinecraftResource());
			vehicleModel.addToTextureResource(minecraftTextureResources);
		});
		bogie2Models.forEach(vehicleModel -> {
			minecraftModelResources.add(vehicleModel.getAsMinecraftResource());
			vehicleModel.addToTextureResource(minecraftTextureResources);
		});
	}

	public boolean hasGangway1() {
		return hasGangway1;
	}

	public boolean hasGangway2() {
		return hasGangway2;
	}

	public boolean hasBarrier1() {
		return hasBarrier1;
	}

	public boolean hasBarrier2() {
		return hasBarrier2;
	}

	public static boolean matchesCondition(VehicleExtension vehicle, PartCondition partCondition, boolean noOpenDoorways) {
		switch (partCondition) {
			case AT_DEPOT:
				return !vehicle.getIsOnRoute();
			case ON_ROUTE_FORWARDS:
				return vehicle.getIsOnRoute() && !vehicle.getReversed();
			case ON_ROUTE_BACKWARDS:
				return vehicle.getIsOnRoute() && vehicle.getReversed();
			case DOORS_CLOSED:
				return vehicle.persistentVehicleData.getDoorValue() == 0 || noOpenDoorways;
			case DOORS_OPENED:
				return vehicle.persistentVehicleData.getDoorValue() > 0 && !noOpenDoorways;
			default:
				return getChristmasLightState(partCondition);
		}
	}

	public void collectTags(Object2ObjectAVLTreeMap<String, Object2ObjectAVLTreeMap<String, ObjectArrayList<String>>> tagMap) {
		tags.forEach(tag -> {
			final String[] tagSplit = tag.split(":");
			if (tagSplit.length == 2) {
				tagMap.computeIfAbsent(tagSplit[0], key -> new Object2ObjectAVLTreeMap<>()).computeIfAbsent(tagSplit[1], key -> new ObjectArrayList<>()).add(id);
			}
		});
	}

	private CachedResource<VehicleResourceCache> cachedVehicleResourceInitializer() {
		return new CachedResource<>(() -> {
			CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.beginReload();

			final ObjectArrayList<Box> floors = new ObjectArrayList<>();
			final Object2ObjectOpenHashMap<PartCondition, ObjectArrayList<OptimizedModelWrapper.MaterialGroupWrapper>> materialGroupsModel = new Object2ObjectOpenHashMap<>();
			final Object2ObjectOpenHashMap<PartCondition, ObjectArrayList<OptimizedModelWrapper.MaterialGroupWrapper>> materialGroupsModelDoorsClosed = new Object2ObjectOpenHashMap<>();
			final Object2ObjectOpenHashMap<PartCondition, ObjectArrayList<OptimizedModelWrapper.MaterialGroupWrapper>> materialGroupsBogie1Model = new Object2ObjectOpenHashMap<>();
			final Object2ObjectOpenHashMap<PartCondition, ObjectArrayList<OptimizedModelWrapper.MaterialGroupWrapper>> materialGroupsBogie2Model = new Object2ObjectOpenHashMap<>();
			final Object2ObjectOpenHashMap<PartCondition, ObjectArrayList<OptimizedModelWrapper.ObjModelWrapper>> objModelsModel = new Object2ObjectOpenHashMap<>();
			final Object2ObjectOpenHashMap<PartCondition, ObjectArrayList<OptimizedModelWrapper.ObjModelWrapper>> objModelsModelDoorsClosed = new Object2ObjectOpenHashMap<>();
			final Object2ObjectOpenHashMap<PartCondition, ObjectArrayList<OptimizedModelWrapper.ObjModelWrapper>> objModelsBogie1Model = new Object2ObjectOpenHashMap<>();
			final Object2ObjectOpenHashMap<PartCondition, ObjectArrayList<OptimizedModelWrapper.ObjModelWrapper>> objModelsBogie2Model = new Object2ObjectOpenHashMap<>();

			final ObjectArrayList<Box> doorways = new ObjectArrayList<>();
			forEachNonNull(models, dynamicVehicleModel -> dynamicVehicleModel.writeFloorsAndDoorways(floors, doorways, materialGroupsModel, materialGroupsModelDoorsClosed, objModelsModel, objModelsModelDoorsClosed));
			forEachNonNull(models, dynamicVehicleModel -> dynamicVehicleModel.modelProperties.iterateParts(modelPropertiesPart -> modelPropertiesPart.mapDoors(doorways)));
			forEachNonNull(bogie1Models, dynamicVehicleModel -> dynamicVehicleModel.writeFloorsAndDoorways(new ObjectArrayList<>(), new ObjectArrayList<>(), new Object2ObjectOpenHashMap<>(), materialGroupsBogie1Model, new Object2ObjectOpenHashMap<>(), objModelsBogie1Model));
			forEachNonNull(bogie2Models, dynamicVehicleModel -> dynamicVehicleModel.writeFloorsAndDoorways(new ObjectArrayList<>(), new ObjectArrayList<>(), new Object2ObjectOpenHashMap<>(), materialGroupsBogie2Model, new Object2ObjectOpenHashMap<>(), objModelsBogie2Model));

			final Object2ObjectOpenHashMap<PartCondition, OptimizedModelWrapper> optimizedModels = writeToOptimizedModels(materialGroupsModel, objModelsModel);
			final Object2ObjectOpenHashMap<PartCondition, OptimizedModelWrapper> optimizedModelsDoorsClosed = writeToOptimizedModels(materialGroupsModelDoorsClosed, objModelsModelDoorsClosed);
			final Object2ObjectOpenHashMap<PartCondition, OptimizedModelWrapper> optimizedModelsBogie1 = writeToOptimizedModels(materialGroupsBogie1Model, objModelsBogie1Model);
			final Object2ObjectOpenHashMap<PartCondition, OptimizedModelWrapper> optimizedModelsBogie2 = writeToOptimizedModels(materialGroupsBogie2Model, objModelsBogie2Model);

			if (floors.isEmpty() && doorways.isEmpty()) {
				Init.LOGGER.info("[{}] No floors or doorways found in vehicle models", id);
				final double x1 = width / 2 + 0.25;
				final double x2 = width / 2 + 0.5;
				final double y = 1 + legacyRiderOffset;
				final double z = length / 2 - 0.5;
				floors.add(new Box(-x1, y, -z, x1, y, z));
				for (double j = -z; j <= z + 0.001; j++) {
					doorways.add(new Box(-x1, y, j, -x2, y, j + 1));
					doorways.add(new Box(x1, y, j, x2, y, j + 1));
				}
			}

			CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.finishReload();
			return new VehicleResourceCache(new ObjectImmutableList<>(floors), new ObjectImmutableList<>(doorways), optimizedModels, optimizedModelsDoorsClosed, optimizedModelsBogie1, optimizedModelsBogie2);
		}, VehicleModel.MODEL_LIFESPAN);
	}

	private Supplier<VehicleSoundBase> createVehicleSoundBaseInitializer() {
		if (bveSoundBaseResource.isEmpty()) {
			final LegacyVehicleSound legacyVehicleSound = new LegacyVehicleSound(
					legacySpeedSoundBaseResource,
					(int) legacySpeedSoundCount,
					legacyUseAccelerationSoundsWhenCoasting,
					legacyConstantPlaybackSpeed,
					legacyDoorSoundBaseResource,
					legacyDoorCloseSoundTime
			);
			return () -> legacyVehicleSound;
		} else {
			final BveVehicleSoundConfig bveVehicleSoundConfig = new BveVehicleSoundConfig(bveSoundBaseResource);
			return () -> new BveVehicleSound(bveVehicleSoundConfig);
		}
	}

	private static void iterateModels(ObjectArrayList<VehicleModel> models, ModelConsumer modelConsumer) {
		for (int i = 0; i < models.size(); i++) {
			final VehicleModel vehicleModel = models.get(i);
			if (vehicleModel != null) {
				final DynamicVehicleModel dynamicVehicleModel = vehicleModel.cachedModel.getData(false);
				if (dynamicVehicleModel != null) {
					modelConsumer.accept(i, dynamicVehicleModel);
				}
			}
		}
	}

	private static boolean getChristmasLightState(PartCondition partCondition) {
		final int index;
		switch (partCondition) {
			case CHRISTMAS_LIGHT_RED:
				index = 0;
				break;
			case CHRISTMAS_LIGHT_YELLOW:
				index = 1;
				break;
			case CHRISTMAS_LIGHT_GREEN:
				index = 2;
				break;
			case CHRISTMAS_LIGHT_BLUE:
				index = 3;
				break;
			default:
				return true;
		}
		return CHRISTMAS_LIGHT_STAGES[(int) ((System.currentTimeMillis() / 500) % CHRISTMAS_LIGHT_STAGES.length)][index];
	}

	private static void queue(Object2ObjectOpenHashMap<PartCondition, OptimizedModelWrapper> optimizedModels, StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int light, boolean noOpenDoorways) {
		optimizedModels.forEach((partCondition, optimizedModel) -> {
			if (matchesCondition(vehicle, partCondition, noOpenDoorways)) {
				MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, offset) -> {
					storedMatrixTransformations.transform(graphicsHolder, offset);
					CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.queue(optimizedModel, graphicsHolder, light);
					graphicsHolder.pop();
				});
			}
		});
	}

	private static Object2ObjectOpenHashMap<PartCondition, OptimizedModelWrapper> writeToOptimizedModels(
			Object2ObjectOpenHashMap<PartCondition, ObjectArrayList<OptimizedModelWrapper.MaterialGroupWrapper>> materialGroupsModel,
			Object2ObjectOpenHashMap<PartCondition, ObjectArrayList<OptimizedModelWrapper.ObjModelWrapper>> objModelsModel
	) {
		final Object2ObjectOpenHashMap<PartCondition, OptimizedModelWrapper> optimizedModels = new Object2ObjectOpenHashMap<>();

		for (final PartCondition partCondition : PartCondition.values()) {
			final OptimizedModelWrapper optimizedModel1;
			final ObjectArrayList<OptimizedModelWrapper.MaterialGroupWrapper> materialGroups = materialGroupsModel.get(partCondition);
			optimizedModel1 = materialGroups == null ? null : OptimizedModelWrapper.fromMaterialGroups(materialGroups);

			final OptimizedModelWrapper optimizedModel2;
			final ObjectArrayList<OptimizedModelWrapper.ObjModelWrapper> objModels = objModelsModel.get(partCondition);
			optimizedModel2 = objModels == null ? null : OptimizedModelWrapper.fromObjModels(objModels);
			optimizedModels.put(partCondition, new OptimizedModelWrapper(optimizedModel1, optimizedModel2));
		}

		return optimizedModels;
	}

	private static void forEachNonNull(ObjectArrayList<VehicleModel> models, Consumer<DynamicVehicleModel> consumer) {
		models.forEach(vehicleModel -> {
			final DynamicVehicleModel dynamicVehicleModel = vehicleModel.cachedModel.getData(true);
			if (dynamicVehicleModel != null) {
				consumer.accept(dynamicVehicleModel);
			}
		});
	}

	@FunctionalInterface
	public interface ModelConsumer {
		void accept(int index, DynamicVehicleModel dynamicVehicleModel);
	}
}
