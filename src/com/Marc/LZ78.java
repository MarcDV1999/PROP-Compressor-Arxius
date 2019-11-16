package com.Marc;

import org.javatuples.Triplet;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


class LZ78 {

    private HashMap<String, Triplet<Integer,Integer,Character>> code = new HashMap<> ();
    private HashMap<Integer, Triplet<String,Integer,Character>> decode = new HashMap<> ();
    private BufferedWriter writer;
    private BinFile binFile;
    private File file;
    private int numFrases = 0;


    public LZ78(String name) throws IOException{
        this.writer = new BufferedWriter(new FileWriter(name));
        this.file = new File(name);
        this.binFile = new BinFile(name);

    }

    // Retorna una Lista de Tuplas(Pointer, UltimoChar) en ASCII.
    private List codify(File file) {
        /*  Pre: text no ha de ser vacio, sino salta una Excepcion
            Post: Llena la HashTable con las frases frequentes en formato LZ78 con la estructura siguiente:

                    {Frase , ID , Pointer , UltimoChar}   Ej ---> {A=[1,0,A]}
                  Guarda en textCodified el texto codificado en formato:
                    [ID , Pointer , UltimoChar]   Ej ---> [1, 0, A][2, 0, B][3, 1, B]

         */
        List textCodifiedASCII = new ArrayList();
        try {
            code.clear();
            String text = file.readFile(file.getTextFileName());
            Integer contadorID = 1; //Se usa para ir asignando ID a las distintas frases en la HashTable
            String frase = Character.toString(text.charAt(0)); // Inicialitzem la primera frase amb el primer char del text
            Triplet tripleta = new Triplet(0, 0, frase.charAt(0)); // Realment no m'importa el valor inicial

            code.put("", new Triplet(0, 0, '?')); // Agregamos el elemento 0 en la HashTable

            // Recorremos text i vamos almacenando todas las frases
            for (int i = 1; i < text.length(); i++) {
               //if(i == text.length()-2) System.out.println("Codifiquem: " + text.charAt(i));
                if (!code.containsKey(frase)) {
                    // Si no tengo la frase, lo agrego y reinicio la variable local frase
                    if (frase.length() > 1) { //Si es compuesta, tengo que buscar la frase a la que referencia
                        //System.out.println("\tFrase llarga: " + frase);
                        Integer v1 = code.get(frase.substring(0, frase.length() - 1)).getValue0(); // Quitamos el ultimo char a la frase para buscar en la HashTable
                        tripleta = new Triplet(contadorID, v1, frase.charAt(frase.length() - 1));

                    } else tripleta = new Triplet(contadorID, 0, frase.charAt(0));

                    code.put(frase, tripleta);
                    contadorID++;
                    textCodifiedASCII.add(tripleta.getValue1().toString());
                    textCodifiedASCII.add(tripleta.getValue2().toString());

                    frase = Character.toString(text.charAt(i)); // Inicialitzem la primera frase amb el primer char del text
                }
                // Si ya lo tenia sigo buscando
                else frase += Character.toString(text.charAt(i)); // Inicialitzem la primera frase amb el primer char del text

            }
            // Ejecutamos esto por si la ultima frase de text era una frase a guardar
            if (!code.containsKey(frase)) {
                 //System.out.println("AQUI ");
                if (frase.length() > 0) {
                    //System.out.println("\tFrase llarga: " + frase);
                    Integer v1 = code.get(frase.substring(0, frase.length() - 1)).getValue0(); // Quitamos el ultimo char a la frase para buscar en la HashTable
                    tripleta = new Triplet(contadorID++, v1, frase.charAt(frase.length() - 1));
                } else tripleta = new Triplet(contadorID++, 0, frase.charAt(0));
                //System.out.println("\tFrase llarga: " + frase);
                code.put(frase, tripleta);
                textCodifiedASCII.add(tripleta.getValue1().toString());
                textCodifiedASCII.add(tripleta.getValue2().toString());
            }
            else if(frase.length() > 0){
                Integer v1 = code.get(frase).getValue0();
                tripleta = new Triplet(contadorID++, v1, '\u001a');
                code.put(frase, tripleta);
                textCodifiedASCII.add(tripleta.getValue1().toString());
                textCodifiedASCII.add(tripleta.getValue2().toString());
            }
            //System.out.println("\tFrase llarga1: " + frase);


            numFrases = --contadorID;
            writer.close();
        }
        catch (Exception e) {
            System.out.println("Ha habido un Error al intentar Codificar el texto. Revise que el documento no este vacio.");
        }
        return textCodifiedASCII;

    }

    private String deCodify(List textCompressedASCII){
        decode.clear();
        String textDescompressed = "";
        int contadorID = 1, pointer;
        String ultimoChar = "";
        //String frase = textDeCodifiedASCII.get(0).toString(); // Inicialitzem la primera frase amb el primer char del text
        String frase = "";
        //System.out.println("Hola: " + textDeCodifiedASCII.get(0));
        //Triplet tripleta = new Triplet(0, 0, frase.charAt(0));

        decode.put(0, new Triplet("", 0, '?')); // Agregamos el elemento 0 en la HashTable
        // Decodificamos Header

        for (int i = 0; i < textCompressedASCII.size(); i+=2){
            pointer = Integer.valueOf(textCompressedASCII.get(i).toString());
            ultimoChar = textCompressedASCII.get(i+1).toString();
            //System.out.println("Analizamos " + pointer + "," + ultimoChar);
            if(pointer == 0) {
                //System.out.println("\tEs 0, Añadimos la letra " + ultimoChar);
                decode.put(contadorID++, new Triplet<String, Integer, Character>(ultimoChar, pointer, ultimoChar.charAt(0)));
                textDescompressed += ultimoChar.charAt(0);
            }
            else{

                frase = decode.get(pointer).getValue0();
                //System.out.println("\tEs FRASE, Añadimos la letra " + frase + ultimoChar);
                decode.put(contadorID++, new Triplet<String, Integer, Character>(frase + ultimoChar, pointer, ultimoChar.charAt(0)));
                textDescompressed += frase + ultimoChar;
            }
            //System.out.println("Text:\t" + textDescompressed + "\n----------------");

        }
        return textDescompressed;
    }


    //Coge el conjunto de tuplas a escribir en el binario y lo escribe en el.
    public void compress(String name) throws IOException {
        List textCodifiedASCII = codify(file);
        binFile.writeBinFile(textCodifiedASCII,binFile.getBinFileName(), numFrases);
        System.out.println("Texto Codificado:\t" + textCodifiedASCII);
    }

    // Coge el texto Comprimido en Binario y lo traduce a un String en ASCII
    public void discompress(String name) throws IOException{
        String decompressedFileName = name + "Unziped.txt";
        List textCodifiedASCII = binFile.readBinFile(binFile.getBinFileName());
        String textDecodified = deCodify(textCodifiedASCII);
        file.writeFile(decompressedFileName, textDecodified);
    }



}


