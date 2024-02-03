package com.batch.springbatch.config;


import com.batch.springbatch.entity.Employee;
import com.batch.springbatch.repository.EmployeeRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    JobBuilderFactory jobBuilderFactory;

    @Autowired
    StepBuilderFactory stepBuilderFactory;




    //reader
    @Bean
    public FlatFileItemReader<Employee> employeeReader(){

        FlatFileItemReader<Employee> fileItemReader = new FlatFileItemReader<>();
        fileItemReader.setName("employee-reader");
        fileItemReader.setResource(new FileSystemResource("src/main/resources/customers.csv"));
        fileItemReader.setLinesToSkip(1);
        fileItemReader.setLineMapper(lineMapper());
        return fileItemReader;
    }

    private LineMapper<Employee> lineMapper() {
        DefaultLineMapper<Employee> defaultLineMapper =new DefaultLineMapper();

        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames("id", "firstName", "lastName", "email", "gender", "contactNo", "country", "dob");



        BeanWrapperFieldSetMapper<Employee> fieldSetMapper = new BeanWrapperFieldSetMapper();
        fieldSetMapper.setTargetType(Employee.class);

        defaultLineMapper.setLineTokenizer(lineTokenizer);
        defaultLineMapper.setFieldSetMapper(fieldSetMapper);

        return defaultLineMapper;

    }

    //processor
    @Bean
    public EmployeeProcessor processor(){
        return new EmployeeProcessor();
    }


    //writer
    @Bean
    public RepositoryItemWriter<Employee> employeeWriter(){
      RepositoryItemWriter<Employee> itemWriter = new RepositoryItemWriter<>();
      itemWriter.setRepository(employeeRepository);
        itemWriter.setMethodName("save");
        return itemWriter;

    }

    @Bean
    public Step step() {
        return stepBuilderFactory.get("step-1").<Employee, Employee>chunk(10)
                .reader(employeeReader())
                .processor(processor())
                .writer(employeeWriter())
                .taskExecutor(taskExecutor())
                .build();
    }

        @Bean
        public Job job() {
            return jobBuilderFactory.get("customers-import")
                    .flow(step())
                    .end()
                    .build();
        }


        @Bean
        public TaskExecutor taskExecutor() {
            SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
            taskExecutor.setConcurrencyLimit(10);
            return taskExecutor;
        }

}
