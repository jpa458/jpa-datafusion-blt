
package com.jpa.cdap.plugins;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.Emitter;
import io.cdap.cdap.etl.api.PipelineConfigurer;
import io.cdap.cdap.etl.api.Transform;
import io.cdap.cdap.etl.api.TransformContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@Plugin(type = Transform.PLUGIN_TYPE)
@Name("BusinessLogicTransformation")
@Description("Transfrom known data record according to business logic rules")
public class BusinessLogicTransformation extends Transform<StructuredRecord, StructuredRecord> {
  // If you want to log things, you will need this line
  private static final Logger LOG = LoggerFactory.getLogger(BusinessLogicTransformation.class);

  // Usually, you will need a private variable to store the config that was passed to your class
  private final Config config;
  private Schema outputSchema;


  public BusinessLogicTransformation(Config config) {
    this.config = config;
  }

  /**
   * This function is called when the pipeline is published. You should use this for validating the config and setting
   * additional parameters in pipelineConfigurer.getStageConfigurer(). Those parameters will be stored and will be made
   * available to your plugin during runtime via the TransformContext. Any errors thrown here will stop the pipeline
   * from being published.
   * @param pipelineConfigurer Configures an ETL Pipeline. Allows adding datasets and streams and storing parameters
   * @throws IllegalArgumentException If the config is invalid.
   */
  @Override
  public void configurePipeline(PipelineConfigurer pipelineConfigurer) throws IllegalArgumentException {
    super.configurePipeline(pipelineConfigurer);
    // It's usually a good idea to validate the configuration at this point. It will stop the pipeline from being
    // published if this throws an error.
    Schema inputSchema = pipelineConfigurer.getStageConfigurer().getInputSchema();
    config.validate(inputSchema);
    try {
      pipelineConfigurer.getStageConfigurer().setOutputSchema(Schema.parseJson(config.schema));
    } catch (IOException e) {
      throw new IllegalArgumentException("Output schema cannot be parsed.", e);
    }
  }

  /**
   * This function is called when the pipeline has started. The values configured in here will be made available to the
   * transform function. Use this for initializing costly objects and opening connections that will be reused.
   * @param context Context for a pipeline stage, providing access to information about the stage, metrics, and plugins.
   * @throws Exception If there are any issues before starting the pipeline.
   */
  @Override
  public void initialize(TransformContext context) throws Exception {
    super.initialize(context);
    outputSchema = Schema.parseJson(config.schema);

  }

  /**
   * This is the method that is called for every record in the pipeline and allows you to make any transformations
   * you need and emit one or more records to the next stage.
   * @param input The record that is coming into the plugin
   * @param emitter An emitter allowing you to emit one or more records to the next stage
   * @throws Exception
   */
  @Override
  public void transform(StructuredRecord input, Emitter<StructuredRecord> emitter) throws Exception {
    // Get all the fields that are in the output schema
    List<Schema.Field> fields = outputSchema.getFields();
    // Create a builder for creating the output record
    StructuredRecord.Builder builder = StructuredRecord.builder(outputSchema);
    // Add all the values to the builder
    for (Schema.Field field : input.getSchema().getFields()) {
      String fieldName = field.getName();
      String inputValue = input.get(fieldName).toString();
      builder.set(fieldName, inputValue);
    }
    // If you wanted to make additional changes to the output record, this might be a good place to do it.

    // Finally, build and emit the record.
    emitter.emit(builder.build());
  }

  /**
   * This function will be called at the end of the pipeline. You can use it to clean up any variables or connections.
   */
  @Override
  public void destroy() {
    // No Op
  }

  /**
   * Your plugin's configuration class. The fields here will correspond to the fields in the UI for configuring the
   * plugin.
   */
  public static class Config extends PluginConfig {

    @Name("fieldsToProcess")
    @Description("Field containing kKV pairs from dropdown")
    @Macro // <- Macro means that the value will be substituted at runtime by the user.
    @Nullable
    private final String fieldsToProcess;

    @Name("schema")
    @Description("Specifies the schema of the records outputted from this plugin.")
    private final String schema;

    public Config(String fieldsToProcess, String schema) {
      this.schema = schema;
      this.fieldsToProcess = fieldsToProcess;
    }

    private void validate(Schema inputSchema) throws IllegalArgumentException {
      // It's usually a good idea to check the schema. Sometimes users edit
      // the JSON config directly and make mistakes.
      try {
        Schema.parseJson(schema);
      } catch (IOException e) {
        throw new IllegalArgumentException("Output schema cannot be parsed.", e);
      }

      // You can use the containsMacro() function to determine if you can validate at deploy time or runtime.
      // If your plugin depends on fields from the input schema being present or the right type, use inputSchema
    }
  }
}

