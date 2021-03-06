/**
 * Copyright (c) 2015-2016 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Bosch Software Innovations GmbH - Please refer to git log
 */
package org.eclipse.vorto.repository.web.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.vorto.repository.account.impl.IUserRepository;
import org.eclipse.vorto.repository.api.ModelId;
import org.eclipse.vorto.repository.api.ModelInfo;
import org.eclipse.vorto.repository.api.ModelType;
import org.eclipse.vorto.repository.api.attachment.Attachment;
import org.eclipse.vorto.repository.core.FileContent;
import org.eclipse.vorto.repository.core.IUserContext;
import org.eclipse.vorto.repository.core.ModelResource;
import org.eclipse.vorto.repository.core.impl.UserContext;
import org.eclipse.vorto.repository.core.impl.parser.ModelParserFactory;
import org.eclipse.vorto.repository.core.impl.utils.ModelValidationHelper;
import org.eclipse.vorto.repository.core.impl.validation.ValidationException;
import org.eclipse.vorto.repository.importer.ValidationReport;
import org.eclipse.vorto.repository.web.AbstractRepositoryController;
import org.eclipse.vorto.repository.web.core.exceptions.UploadTooLargeException;
import org.eclipse.vorto.repository.web.core.templates.ModelTemplate;
import org.eclipse.vorto.repository.workflow.IWorkflowService;
import org.eclipse.vorto.repository.workflow.WorkflowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Alexander Edelmann - Robert Bosch (SEA) Pte. Ltd.
 */
@RestController("internal.modelRepositoryController")
@RequestMapping(value = "/rest/models")
public class ModelRepositoryController extends AbstractRepositoryController  {


	@Value("${repo.config.maxModelImageSize}")
	private long maxModelImageSize;
	
	@Autowired
	private IUserRepository userRepository;

	@Autowired
	private IWorkflowService workflowService;

	private static Logger logger = Logger.getLogger(ModelRepositoryController.class);

	@ApiOperation(value = "Returns the image of a vorto model")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Wrong input"),
			@ApiResponse(code = 404, message = "Model not found") })
	@RequestMapping(value = "/{modelId:.+}/images", method = RequestMethod.GET)
	public void getModelImage(
			@ApiParam(value = "The modelId of vorto model, e.g. com.mycompany.Car:1.0.0", required = true) final @PathVariable String modelId,
			@ApiParam(value = "Response", required = true) final HttpServletResponse response) {
		Objects.requireNonNull(modelId, "modelId must not be null");

		final ModelId modelID = ModelId.fromPrettyFormat(modelId);
		List<Attachment> imageAttachments = modelRepository.getAttachmentsByTag(modelID, Attachment.TAG_IMAGE);
		if (imageAttachments.isEmpty()) {
			response.setStatus(404);
			return;
		}
		response.setHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + modelID.getName() + ".png");
		response.setContentType(APPLICATION_OCTET_STREAM);
		try {
			FileContent imageContent = modelRepository.getAttachmentContent(modelID, imageAttachments.get(0).getFilename()).get();
			IOUtils.copy(new ByteArrayInputStream(imageContent.getContent()), response.getOutputStream());
			response.flushBuffer();
		} catch (IOException e) {
			throw new RuntimeException("Error copying file.", e);
		}
	}
	
	@RequestMapping(value = "/{modelId:.+}/images", method = RequestMethod.POST)
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public void uploadModelImage(	@ApiParam(value = "The image to upload", required = true)	@RequestParam("file") MultipartFile file,
									@ApiParam(value = "The model ID of vorto model, e.g. com.mycompany.Car:1.0.0", required = true) final @PathVariable String modelId) {
		if (file.getSize() > maxModelImageSize) {
			throw new UploadTooLargeException("model image", maxModelImageSize);
		}
		
		logger.info("uploadImage: [" + file.getOriginalFilename() + ", " + file.getSize() + "]");
		
		try {
			IUserContext user = UserContext
					.user(SecurityContextHolder.getContext().getAuthentication().getName());
			
			modelRepository.attachFile(ModelId.fromPrettyFormat(modelId),new FileContent(file.getOriginalFilename(),file.getBytes()),user,Attachment.TAG_IMAGE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
		
	@ApiOperation(value = "Saves a model to the repository.")
	@PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
	@RequestMapping(method = RequestMethod.PUT, value = "/{modelId:.+}", produces = "application/json")
	public ValidationReport saveModel(@ApiParam(value = "modelId", required = true) @PathVariable String modelId,
			@RequestBody ModelContent content) {
		try {
			ModelId modelID = ModelId.fromPrettyFormat(modelId);

			IUserContext userContext = UserContext
					.user(SecurityContextHolder.getContext().getAuthentication().getName());
			ModelResource modelInfo = (ModelResource)ModelParserFactory
					.getParser("model" + ModelType.valueOf(content.getType()).getExtension())
					.parse(new ByteArrayInputStream(content.getContentDsl().getBytes()));

			if (!modelID.equals(modelInfo.getId())) {
				return ValidationReport.invalid(modelInfo,
						"You may not change the model ID (name, namespace, version). For this please create a new model.");
			}
			ModelValidationHelper validationHelper = new ModelValidationHelper(modelRepository, userRepository);
			ValidationReport validationReport = validationHelper.validate(modelInfo, userContext);
			if (validationReport.isValid()) {
				this.modelRepository.save(modelInfo.getId(), content.getContentDsl().getBytes(),
						modelInfo.getId().getName() + modelInfo.getType().getExtension(), userContext);
			}
			return validationReport;
		} catch (ValidationException validationException) {
			return ValidationReport.invalid(null, validationException.getMessage());
		}

	}

	@ApiOperation(value = "Creates a model in the repository with the given model ID and model type.")
	@RequestMapping(method = RequestMethod.POST, value = "/{modelId:.+}/{modelType}", produces = "application/json")
	public ResponseEntity<ModelInfo> createModel(@ApiParam(value = "modelId", required = true) @PathVariable String modelId,
			@ApiParam(value = "modelType", required = true) @PathVariable ModelType modelType)
			throws WorkflowException {

		final ModelId modelID = ModelId.fromPrettyFormat(modelId);
		if (this.modelRepository.getById(modelID) != null) {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		} else {
			ModelTemplate template = new ModelTemplate();
			IUserContext userContext = UserContext
					.user(SecurityContextHolder.getContext().getAuthentication().getName());
			ModelInfo savedModel = this.modelRepository.save(modelID,
					template.createModelTemplate(modelID, modelType).getBytes(),
					modelID.getName() + modelType.getExtension(), userContext);
			this.workflowService.start(modelID);
			return new ResponseEntity<>(savedModel,HttpStatus.CREATED);

		}
	}
	
	@RequestMapping(value = "/{modelId:.+}", method = RequestMethod.DELETE)
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(T(org.eclipse.vorto.repository.api.ModelId).fromPrettyFormat(#modelId),'model:delete')")
	public void deleteModelResource(final @PathVariable String modelId) {
		Objects.requireNonNull(modelId, "modelId must not be null");
		this.modelRepository.removeModel(ModelId.fromPrettyFormat(modelId));
	}
	
	
	// ##################### Downloads ################################

	@RequestMapping(value = { "/mine/download" }, method = RequestMethod.GET)
	public void getUserModels(Principal user, final HttpServletResponse response) {
		List<ModelInfo> userModels = this.modelRepository
				.search("author:" + UserContext.user(user.getName()).getHashedUsername());

		logger.info("Exporting information models for " + user.getName() + " results: " + userModels.size());

		sendAsZipFile(response, user.getName() + "-models.zip", userModels);
	}
	
	@ApiOperation(value = "Getting all mapping resources for the given model")
	@PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN') or hasPermission(T(org.eclipse.vorto.repository.api.ModelId).fromPrettyFormat(#modelId),'model:get')")
	@RequestMapping(value = "/{modelId:.+}/download/mappings/{targetPlatform}", method = RequestMethod.GET)
	public void downloadMappingsForPlatform(
			@ApiParam(value = "The model ID of vorto model, e.g. com.mycompany.Car:1.0.0", required = true) final @PathVariable String modelId,
			@ApiParam(value = "The name of target platform, e.g. lwm2m", required = true) final @PathVariable String targetPlatform,
			final HttpServletResponse response) {
		Objects.requireNonNull(modelId, "model ID must not be null");

		final ModelId modelID = ModelId.fromPrettyFormat(modelId);

		List<ModelInfo> mappingResources = modelRepository.getMappingModelsForTargetPlatform(modelID, targetPlatform);

		final String fileName = modelID.getNamespace() + "_" + modelID.getName() + "_" + modelID.getVersion() + ".zip";

		sendAsZipFile(response, fileName, mappingResources);
	}
}
