package regression.reinforce;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.accum.Max;
import org.nd4j.linalg.api.ops.impl.indexaccum.IAMax;
import org.nd4j.linalg.api.ops.impl.scalar.ScalarMax;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import org.nd4j.linalg.dataset.DataSet;
import java.util.List;
import java.util.Collections;
import java.util.Random;

import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
/**
 * Created by william on 25/03/16.
 */
public class DeepQ {

    public MultiLayerNetwork net;
    public double gamma;
    public double lrate;
    public int na;
    public double[] position;
    public double[] velocity;
    public int t=0;

    public DeepQ(MultiLayerNetwork net, double gamma, double lrate, int na, double[] position, double[] velocity) {
        this.net = net;
        this.gamma = gamma;
        this.lrate = lrate;
        this.na = na;
        this.position = position;
        this.velocity = velocity;
    }


    public void update(INDArray states_input, INDArray actions,
                  INDArray next_states, INDArray rewards, INDArray eoes,
                  int batchSize, Random rng , int nEpochs){ //update le reseau avec les inputs donnes,
                    // et les actions suivantes possibles (fait un fitting)
        // c est la grosse fonction du programme
        INDArray states_input_copy = normalize(states_input.dup().transpose());
        int N = states_input_copy.shape()[0];
        INDArray bestnextvalues = Nd4j.zeros(1,N);//le max selon toutes les values de next_state
        INDArray gactions =  Nd4j.zeros(1,N);
        INDArray betterQvalues;
        if (t==0){
            betterQvalues = rewards.dup();
        }
        else {
            this.predict(normalize(next_states), bestnextvalues, gactions); // il y a un pb a regler avec l action
            betterQvalues = rewards.dup().add(bestnextvalues.dup().mul(- gamma).muli(eoes.dup().sub(1))); //todo: verifier s il y a bien une copie
        }

        //todo: c est une maniere de prendre en compte les eoes mais je sais pas si ca marche
        //todo: pas forcement besoin de reward.dup()


        //INDArray betterQvalues  = this.net.output(next_states); //todo: voir pcq il y a un pb avec
        // les actions
        /*todo: je pense que
        il y a beaucoup trop de Qold dans l affaire ca peut se simplifier mais pour l instant on laisse
        */
        //todo: aussi il faut que je change tous les tableaux en nd4j je pense

        //calculer une bonne fois pour toutes le tableau, si lrate ==1
//        INDArray qValueToPut = Nd4j.zeros(1, N);
//        if(lrate ==1) {
////            for (int k = 0; k < N; k++) { //parcourir chaque transition
////                int index_action = (int) actions.getDouble(0, k);
////                qValueToPut.put(0,k,betterQvalues.getDouble(0, k));
////            }
//            qValueToPut.putRow(0, betterQvalues.getRow(0));
//        }

        INDArray totalLabels = Nd4j.zeros(N,2);
        totalLabels.putColumn(0,betterQvalues); //si ca marche pas, transposer
        totalLabels.putColumn(1, actions);
        DataSet data = new DataSet(states_input_copy, totalLabels);
//        data.shuffle(); //todo: faire attention parce que ici je le shuffle partout, c est peut etre bizarre
        ReinforcementDataSetIterator it = new ReinforcementDataSetIterator(data, batchSize, net);

        for (int i = 0; i < nEpochs ;i++) {
            //je dois avoir une dataset ici

//
//        INDArray oldQvalues = this.net.output(states_input_copy).transpose();
//        INDArray newQvalues = oldQvalues.dup();
//               //todo: if ( c est l action choisie):
//
//
//        for (int k = 0; k<N; k++) { //parcourir chaque transition
//            int index_action = (int) actions.getDouble(0, k);
//            if( lrate != 1){
//            qValueToPut.put(0,k,oldQvalues.getDouble(index_action, k) + betterQvalues.getDouble(0, k)
//                    - oldQvalues.getDouble(index_action, k) * lrate);
//            }
//            newQvalues.put(index_action, k, qValueToPut.getDouble(0,k)); //todo: simplify, and take in account eoes
//        }

//            //todo : absolumen simplifier ce point il y a de la redondance
//
//        //TODO absolument !!!
//        //TODO: faire en sorte de regler quand on donne la reward
////        System.out.println(oldQvalues.shape()[0]);
////        System.out.println(oldQvalues.shape()[1]);
////        System.out.println(newQvalues.shape()[0]);
////        System.out.println(newQvalues.shape()[1]);
//
//            //iterator.reset(); // todo: voir si je le mets ou pas
////
////            System.out.println("Prining states some input");
////            for(int co =0; co<N; co++){
////                double posit= states_input_copy.getDouble(co, 0);
////                double veloc= states_input_copy.getDouble(co, 1);
////                if (Math.abs(posit )>1 || Math.abs(veloc )>1){
////                    System.out.println("veloc = " + veloc);
////                    System.out.println("posit = " + posit);
////                }
////            }
////            System.out.println("Printing some newQvalues");
////            for(int co =0; co<N; co++){
////                double newq = newQvalues.getDouble(1, co);
////                if (Math.abs(newq)> 1){
////                    System.out.println("newq = " + newq);
////                }
////            }
////
////
//
//            DataSetIterator iterator = getTrainingData(states_input_copy,newQvalues.transpose(), batchSize,rng);
//            //for (int ne =0 ; ne<nEpoch; n++){
            //iter.next(); // ou while iter.hasnNext();
            //net.fit(iter);
            //}
            net.fit(it);
        }
        t++;

    }

    public void predict(INDArray states, INDArray v, INDArray gactions){ //todo: doit retourner les values et les greedy actions en tout cas dans le python
        /*
        int N = states.shape()[1];
        // todo : initialiser  a 0 les reward et les eoe meme si on s en fout
        //todo : verifier que je mets bien des nd4j partout
        INDArray rewards = Nd4j.zeros(1, N);
        //int[] rewards = new int[N];
        INDArray eoes = Nd4j.zeros(1, N);
        //boolean[] eoes = new boolean[N];
        //return this.net.output(next_states);
        for (int k = 0; k<na; k++) {
            int action = k;
            INDArray action_tab = Nd4j.ones(1,N).mul(k);
            //INDArray next_next_states = mountain_car.transition(next_states, action_tab, rewards, eoes); //todo: regler le pb du non static
            //compare avec le precedent vecteur et garde uniquement la valeur la plus
            //grande et l action correspondante
            INDArray newValue = net.output(next_states.transpose()).transpose(); // ici j assimile state, a , a next_state //todo: enlever les transpose et remettre bien
            for (int p = 0; p<N ; p++){ //todo: regler length ca doit etre shape ou qqch comme ca
                System.out.println(v.shape()[0]);
                System.out.println(newValue.shape()[0]);
                if (v.getDouble(k,p) <  newValue.getDouble(k,p)) {
                    v.put(0,p, newValue.getDouble(k, p));
                    gactions.put(0, p, k);
                }
            } //on a notre v et notre gactions
        }
        */

        /*
        WARNING: the input should be normalized
        */
        INDArray newValues = net.output(states.dup().transpose()).transpose(); //verif shape
        System.out.println(newValues.shape()[0]);
        System.out.println(newValues.shape()[1]);

        v.putRow(0,Nd4j.getExecutioner().exec(new Max(newValues.dup()), 0));
        gactions.putRow(0,Nd4j.getExecutioner().exec(new IAMax(newValues.dup()), 0));


    }

    public void train_net(int nEpochs, DataSetIterator iterator) {
        //Train the network on the full data set, and evaluate in periodically
        for (int i = 0; i < nEpochs; i++) {
            //iterator.reset(); // todo: voir si je le mets ou pas
            net.fit(iterator);
        }
    }


    private static DataSetIterator getTrainingData(final INDArray x, final INDArray y, final int batchSize, final Random rng) {
        final DataSet allData = new DataSet(x,y);
        final List<DataSet> list = allData.asList();
        Collections.shuffle(list,rng);
        return new ListDataSetIterator(list,batchSize);

    }



    public INDArray normalize(INDArray states_input){ //le INDArray doit etre oriente en vertical
        INDArray state = states_input.dup();
        state.getColumn(0).subi(position[0]).divi(position[1]-position[0]);
        state.getColumn(1).subi(velocity[0]).divi(velocity[1]-velocity[0]);
        return state;
    }



}

