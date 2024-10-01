package municipio;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

public class BancoDeDados {

    private Connection conexao;

    public void conectar() throws ClassNotFoundException, SQLException {
        System.out.println("Conectando no banco");
        Class.forName("com.mysql.cj.jdbc.Driver");
        conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/techmentor?useUnicode=true&characterEncoding=UTF-8", "root", "root");
        conexao.setAutoCommit(false);
        System.out.println("Banco conectado");
    }

    public void fecharConexao() throws SQLException {
        if (conexao != null && !conexao.isClosed()) {
            conexao.close();
            System.out.println("Conexão fechada.");
        } else {
            System.out.println("A conexão já está fechada ou é nula.");
        }
    }

    public void inserirDados(List<List<Object>> dadosExcel) throws SQLException {
        if (conexao == null) {
            throw new SQLException("Conexão com o banco de dados não foi estabelecida.");
        }

        String query = "INSERT INTO municipio (ano, cidade, operadora, domiciliosCobertosPercent, areaCobertaPercent, tecnologia) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
//        Truncar tabela antes de inserir
        System.out.println("Truncando a tabela municipio...");
        String truncateQuery = "TRUNCATE TABLE municipio";
        Statement statement = conexao.createStatement();
        statement.executeUpdate(truncateQuery);
        System.out.println("Tabela Truncada com sucesso!");
        System.out.println("Preparando para inserir dados");
        PreparedStatement preparedStatement = conexao.prepareStatement(query);

        // Pular a primeira linha (cabeçalho)
        for (int i = 1; i < dadosExcel.size(); i++) {  // Começar em 1 para pular o cabeçalho
            List<Object> row = dadosExcel.get(i);

            // Converter linha para String e aplicar split
            String linha = convertRowToString(row);

            // Fazer o split da linha para dividir os campos
            String[] valores = linha.split(";");



            // Verifica se o número de campos corresponde ao esperado
            if (valores.length < 13) {
                System.err.println("Linha com menos colunas do que o esperado. Ignorando: " + linha);
                continue;  // Pular se a linha não tiver a quantidade esperada de colunas
            }

            // Inserindo os dados splitados no banco
            preparedStatement.setString(1, getSafeValue(valores, 0)); // ano
            preparedStatement.setString(2, getSafeValue(valores, 5));  // cidade
            preparedStatement.setString(3, getSafeValue(valores, 2)); // operadora

            preparedStatement.setBigDecimal(4, convertToBigDecimal(getSafeValue(valores, 11)));  // % domicílios cobertos
            preparedStatement.setBigDecimal(5, convertToBigDecimal(getSafeValue(valores, 12)));  // % área coberta
            System.out.println(valores[i]);



            // Obtenha o valor da tecnologia e aplique a formatação dinâmica
            String tecnologiaFormatada = formatarTecnologia(getSafeValue(valores, 3));
            preparedStatement.setString(6, tecnologiaFormatada);  // tecnologia


            // Executar a query para a linha atual
            preparedStatement.addBatch();
            conexao.commit();

            if(i % 5000 == 0) {
            preparedStatement.executeBatch();
            }
        }

        System.out.println("Dados Inseridos");
        preparedStatement.close();
    }

    /**
     * Converte uma lista de objetos para uma string, unindo com separador ';'.
     */
    private String convertRowToString(List<Object> row) {
        StringBuilder linha = new StringBuilder();

        for (Object celula : row) {
            if (linha.length() > 0) {
                linha.append(";");  // Adicionar separador entre os valores
            }
            linha.append(celula != null ? celula.toString() : "");  // Converte o Object para String
        }

        return linha.toString();
    }

    /**
     * Método auxiliar para obter um valor de forma segura a partir de um array.
     * Retorna null se o índice estiver fora dos limites ou o valor for vazio.
     */
    private String getSafeValue(String[] valores, int index) {
        return (index < valores.length && !valores[index].isEmpty()) ? valores[index] : null;
    }


    private BigDecimal convertToBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return BigDecimal.ZERO;  // Retorna 0 se o valor for nulo ou vazio
        }
        try {
            // Substitui vírgulas por pontos, se necessário (tratamento de separador decimal)
            value = value.replace(",", ".");
            return new BigDecimal(value);  // Converte para BigDecimal
        } catch (NumberFormatException e) {
            // Retorna 0 se ocorrer um erro de conversão
            return BigDecimal.ZERO;
        }
    }
    private String formatarTecnologia(String tecnologia) {
        if (tecnologia == null || tecnologia.isEmpty()) {
            return "";  // Se a tecnologia for nula ou vazia, retorna uma string vazia
        }

        // Array com as possíveis tecnologias
        String[] possiveisTecnologias = {"2G", "3G", "4G", "5G"};

        // StringBuilder para montar o resultado final
        StringBuilder tecnologiasFormatadas = new StringBuilder();

        // Itera sobre cada tecnologia possível
        for (String tech : possiveisTecnologias) {
            // Verifica se a string original contém a tecnologia atual
            if (tecnologia.contains(tech)) {
                if (tecnologiasFormatadas.length() > 0) {
                    tecnologiasFormatadas.append(", ");  // Adiciona separador somente se não for o primeiro item
                }
                tecnologiasFormatadas.append(tech);  // Adiciona a tecnologia
            }
        }

        return tecnologiasFormatadas.toString();  // Retorna as tecnologias formatadas
    }

    }


