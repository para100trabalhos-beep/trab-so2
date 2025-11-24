# trab-so2
roteiro para acesso genérico ao ec2
inicie o lab
va em details desça com o scroll na mesma pagina e baixe o PEM
ai aperte no icone da AWS 
va no projeto
e procure o ip publico
copie o ip publico
ai va no powershell
escreva 
cd "caminho\até\a\pasta\download"
sem aspas é claro
ai escreva
ssh -i .\labuser.pem ubuntu@COLEOIPAQUI
escreva yes e aperte enter
vitória vc abriu a instancia

parabens vc se tornou 1% mais homem
agr só falta os outros 99

agora escreva em ordem
sudo apt update
sudo apt install -y openjdk-17-jdk git
java -version
javac -version
git --version
https://github.com/SEU_USUARIO/trabalho-sistemas-operacionais.git
git clone https://github.com/para100trabalhos-beep/trab-so2.git
cd trab-so2
ls
tem que aparecer
DiningPhilosophers.java  input_filosofos.txt
javac DiningPhilosophers.java
java DiningPhilosophers input_filosofos.txt
