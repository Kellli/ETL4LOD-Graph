package br.ufrj.ppgi.greco.lodbr.plugin.triplemapping;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

import br.ufrj.ppgi.greco.kettle.plugin.tools.datatable.DataTable;
import br.ufrj.ppgi.greco.kettle.plugin.tools.datatable.DataTableConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ObjectPropertyMappingStepMeta extends BaseStepMeta implements
        StepMetaInterface
{

    // Fields for serialization
    public enum Field
    {
        DATA_ROOT_NODE, VERSION,

        // Aba 'Mapeamento'
        MAP_TABLE,
        MAP_TABLE_SUBJECT_FIELD_NAME,
        MAP_TABLE_PREDICATE_FIELD_NAME, // Permite que predicado venha no fluxo.
                                        // Nao implementado nesta versao
        MAP_TABLE_PREDICATE_URI,
        MAP_TABLE_OBJECT_FIELD_NAME,

        // Aba 'Campos de saida'
        SUBJECT_OUT_FIELD_NAME,
        PREDICATE_OUT_FIELD_NAME,
        OBJECT_OUT_FIELD_NAME,
        KEEP_INPUT_FIELDS,

        // Aba 'Sparql Endpoint' - no futuro
        ENDPOINT_URI,
        DEFAULT_GRAPH,
        PREFIX_TABLE,
        PREFIX_TABLE_PREFIX,
        PREFIX_TABLE_NAMESPACE
    }

    // Campos do step
    private DataTable<String> mapTable;

    private String subjectOutputFieldName;
    private String predicateOutputFieldName;
    private String objectOutputFieldName;
    private boolean keepInputFields;

    // private String endpointUri;
    // private String defaultGraph;
    // private DataTable <String> prefixes;

    public ObjectPropertyMappingStepMeta()
    {
        setDefault();
    }

    // TODO Validar todos os campos para dar feedback ao usuario!
    @Override
    public void check(List<CheckResultInterface> remarks, TransMeta transMeta,
            StepMeta stepMeta, RowMetaInterface prev, String[] input,
            String[] output, RowMetaInterface info)
    {

        // if (Const.isEmpty(fieldName)) {
        // CheckResultInterface error = new CheckResult(
        // CheckResult.TYPE_RESULT_ERROR,
        // "error",
        // stepMeta);
        // remarks.add(error);
        // }
        // else {
        CheckResultInterface ok = new CheckResult(CheckResult.TYPE_RESULT_OK,
                "", stepMeta);
        remarks.add(ok);
        // }
    }

    @Override
    public StepInterface getStep(StepMeta stepMeta,
            StepDataInterface stepDataInterface, int copyNr,
            TransMeta transMeta, Trans trans)
    {
        return new ObjectPropertyMappingStep(stepMeta, stepDataInterface,
                copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData()
    {
        return new ObjectPropertyMappingStepData();
    }

    @Override
    public String getDialogClassName()
    {
        return ObjectPropertyMappingStepDialog.class.getName();
    }

    // Carregar campos a partir do XML de um .ktr
    @SuppressWarnings("unchecked")
    @Override
    public void loadXML(Node stepDomNode, List<DatabaseMeta> databases,
            Map<String, Counter> sequenceCounters) throws KettleXMLException
    {

        try
        {
            XStream xs = new XStream(new DomDriver());
            xs.alias("DataTable", DataTable.class);
            xs.registerConverter(new DataTableConverter());

            StringWriter sw = new StringWriter();
            Transformer t = TransformerFactory.newInstance().newTransformer();
            // IPC: se algum colocar um caracter, seja qual for, no getXML() o
            // getFirstChild() para de funcionar aqui!
            t.transform(
                    new DOMSource(XMLHandler.getSubNode(stepDomNode,
                            Field.DATA_ROOT_NODE.name()).getFirstChild()),
                    new StreamResult(sw));

            Map<String, Object> data = (Map<String, Object>) xs.fromXML(sw
                    .toString());

            int version = (int) (Integer) data.get(Field.VERSION.name());

            switch (version)
            {
            case 1:
                mapTable = (DataTable<String>) data.get(Field.MAP_TABLE.name());

                subjectOutputFieldName = (String) data
                        .get(Field.SUBJECT_OUT_FIELD_NAME.name());
                predicateOutputFieldName = (String) data
                        .get(Field.PREDICATE_OUT_FIELD_NAME.name());
                objectOutputFieldName = (String) data
                        .get(Field.OBJECT_OUT_FIELD_NAME.name());
                keepInputFields = (Boolean) data.get(Field.KEEP_INPUT_FIELDS
                        .name());

                // endpointUri = (String) data.get(Field.ENDPOINT_URI.name());
                // defaultGraph = (String) data.get(Field.DEFAULT_GRAPH.name());
                // prefixes = (DataTable <String>)
                // data.get(Field.PREFIX_TABLE.name());

                break;
            default:
                setDefault();
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    // Gerar XML para salvar um .ktr
    @Override
    public String getXML() throws KettleException
    {

        XStream xs = new XStream();
        xs.alias("DataTable", DataTable.class);
        xs.registerConverter(new DataTableConverter());

        Map<String, Object> data = new HashMap<String, Object>();

        data.put(Field.VERSION.name(), 1);

        data.put(Field.MAP_TABLE.name(), mapTable);

        data.put(Field.SUBJECT_OUT_FIELD_NAME.name(), subjectOutputFieldName);
        data.put(Field.PREDICATE_OUT_FIELD_NAME.name(),
                predicateOutputFieldName);
        data.put(Field.OBJECT_OUT_FIELD_NAME.name(), objectOutputFieldName);
        data.put(Field.KEEP_INPUT_FIELDS.name(), keepInputFields);

        return String.format("<%1$s>%2$s</%1$s>", Field.DATA_ROOT_NODE.name(),
                xs.toXML(data));
    }

    // Rogers (2012): Carregar campos a partir do repositorio
    @Override
    public void readRep(Repository repository, ObjectId stepIdInRepository,
            List<DatabaseMeta> databases, Map<String, Counter> sequenceCounters)
            throws KettleException
    {
        try
        {
            int version = (int) repository.getStepAttributeInteger(
                    stepIdInRepository, Field.VERSION.name());

            switch (version)
            {
            case 1:
                int nrLines = (int) repository.getStepAttributeInteger(
                        stepIdInRepository, "nr_lines");
                mapTable = new DataTable<String>(Field.MAP_TABLE.name(),
                        Field.MAP_TABLE_SUBJECT_FIELD_NAME.name(),
                        Field.MAP_TABLE_PREDICATE_FIELD_NAME.name(),
                        Field.MAP_TABLE_PREDICATE_URI.name(),
                        Field.MAP_TABLE_OBJECT_FIELD_NAME.name());
                String[] fields = mapTable.getHeader().toArray(new String[0]);
                for (int i = 0; i < nrLines; i++)
                {
                    int nrfields = fields.length;
                    String[] line = new String[nrfields];

                    for (int f = 0; f < nrfields; f++)
                    {
                        line[f] = repository.getStepAttributeString(
                                stepIdInRepository, i, fields[f]);
                    }
                    mapTable.add(line);
                }

                subjectOutputFieldName = repository
                        .getStepAttributeString(stepIdInRepository,
                                Field.SUBJECT_OUT_FIELD_NAME.name());
                predicateOutputFieldName = repository.getStepAttributeString(
                        stepIdInRepository,
                        Field.PREDICATE_OUT_FIELD_NAME.name());
                objectOutputFieldName = repository.getStepAttributeString(
                        stepIdInRepository, Field.OBJECT_OUT_FIELD_NAME.name());
                keepInputFields = repository.getStepAttributeBoolean(
                        stepIdInRepository, Field.KEEP_INPUT_FIELDS.name());

                break;
            default:
                setDefault();
            }
        }
        catch (Exception e)
        {
            throw new KettleException(
                    "Unable to read step information from the repository for id_step="
                            + stepIdInRepository, e);
        }

    }

    // Rogers (2012): Persistir campos no repositorio
    @Override
    public void saveRep(Repository repository, ObjectId idOfTransformation,
            ObjectId idOfStep) throws KettleException
    {
        try
        {
            repository.saveStepAttribute(idOfTransformation, idOfStep,
                    Field.VERSION.name(), 1);

            // Map Table
            int linhas = (int) mapTable.size();
            int colunas = mapTable.getHeader().size();
            repository.saveStepAttribute(idOfTransformation, idOfStep,
                    "nr_lines", linhas);
            for (int i = 0; i < linhas; i++)
            {
                for (int f = 0; f < colunas; f++)
                {
                    repository.saveStepAttribute(idOfTransformation, idOfStep,
                            i, mapTable.getHeader().get(f),
                            mapTable.getValue(i, f));
                }
            }

            repository
                    .saveStepAttribute(idOfTransformation, idOfStep,
                            Field.SUBJECT_OUT_FIELD_NAME.name(),
                            subjectOutputFieldName);
            repository.saveStepAttribute(idOfTransformation, idOfStep,
                    Field.PREDICATE_OUT_FIELD_NAME.name(),
                    predicateOutputFieldName);
            repository.saveStepAttribute(idOfTransformation, idOfStep,
                    Field.OBJECT_OUT_FIELD_NAME.name(), objectOutputFieldName);
            repository.saveStepAttribute(idOfTransformation, idOfStep,
                    Field.KEEP_INPUT_FIELDS.name(), keepInputFields);
        }
        catch (Exception e)
        {
            throw new KettleException(
                    "Unable to save step information to the repository for id_step="
                            + idOfStep, e);
        }
    }

    // Inicializacoes default
    @Override
    public void setDefault()
    {

        mapTable = new DataTable<String>(Field.MAP_TABLE.name(),
                Field.MAP_TABLE_SUBJECT_FIELD_NAME.name(),
                Field.MAP_TABLE_PREDICATE_FIELD_NAME.name(),
                Field.MAP_TABLE_PREDICATE_URI.name(),
                Field.MAP_TABLE_OBJECT_FIELD_NAME.name());

        subjectOutputFieldName = "subject";
        predicateOutputFieldName = "predicate";
        objectOutputFieldName = "object";
        keepInputFields = false;
    }

    /**
     * It describes what each output row is going to look like
     */
    @Override
    public void getFields(RowMetaInterface inputRowMeta, String name,
            RowMetaInterface[] info, StepMeta nextStep, VariableSpace space)
            throws KettleStepException
    {
        ValueMeta field = null;

        if (!keepInputFields)
            inputRowMeta.clear();

        field = new ValueMeta(subjectOutputFieldName,
                ValueMetaInterface.TYPE_STRING);
        field.setOrigin(name);
        inputRowMeta.addValueMeta(field);

        field = new ValueMeta(predicateOutputFieldName,
                ValueMetaInterface.TYPE_STRING);
        field.setOrigin(name);
        inputRowMeta.addValueMeta(field);

        field = new ValueMeta(objectOutputFieldName,
                ValueMetaInterface.TYPE_STRING);
        field.setOrigin(name);
        inputRowMeta.addValueMeta(field);
    }

    // Getters & Setters
    public DataTable<String> getMapTable()
    {
        return mapTable;
    }

    public String getSubjectOutputFieldName()
    {
        return subjectOutputFieldName;
    }

    public void setSubjectOutputFieldName(String subjectOutputFieldName)
    {
        this.subjectOutputFieldName = subjectOutputFieldName;
    }

    public String getPredicateOutputFieldName()
    {
        return predicateOutputFieldName;
    }

    public void setPredicateOutputFieldName(String predicateOutputFieldName)
    {
        this.predicateOutputFieldName = predicateOutputFieldName;
    }

    public String getObjectOutputFieldName()
    {
        return objectOutputFieldName;
    }

    public void setObjectOutputFieldName(String objectOutputFieldName)
    {
        this.objectOutputFieldName = objectOutputFieldName;
    }

    public boolean isKeepInputFields()
    {
        return keepInputFields;
    }

    public void setKeepInputFields(boolean keepInputFields)
    {
        this.keepInputFields = keepInputFields;
    }
}
